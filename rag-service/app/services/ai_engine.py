"""
AI engine for RAG retrieval and answer generation using Ollama
"""
import ollama
from app.core.config import get_settings
from app.core.constants import RESPOND_TO_MESSAGE_SYSTEM_PROMPT
from app.core.db import db

settings = get_settings()

def get_vector_context(query_text: str, top_k: int = None) -> str:
    """
    Finds the most relevant chunks using HNSW index.
    """

    if top_k is None:
        top_k = settings.max_chunks

    print(f"\n{'='*60}")
    print(f"DEBUG: SEARCHING FOR: '{query_text}'")
    print(f"{'='*60}")

    try:
        ollama_client = ollama.Client(host='http://localhost:11434')

        print("DEBUG: Generating query embedding...")
        response = ollama_client.embed(
            model=settings.embedding_model,
            input=query_text
        )
        query_embedding = response['embeddings'][0]
        print(f"DEBUG: Generated embedding: {len(query_embedding)} dimensions")

        vector_str = '[' + ','.join(map(str, query_embedding)) + ']'

        # Check total chunks
        count_cursor = db.execute_sql("SELECT COUNT(*) FROM document_information_chunks;")
        total_chunks = count_cursor.fetchone()[0]
        print(f"DEBUG: Total chunks in database: {total_chunks}")

        print(f"DEBUG: Running vector similarity search (top {top_k})...")
        cursor = db.execute_sql("""
                                SELECT chunk, embedding <=> %s::vector as distance
                                FROM document_information_chunks
                                ORDER BY embedding <=> %s::vector
                                LIMIT %s;
                                """, (vector_str, vector_str, top_k))

        results = cursor.fetchall()
        print(f"DEBUG: Found {len(results)} results")

        if results:
            for i, (chunk, distance) in enumerate(results, 1):
                print(f"\nDEBUG: Result {i} (distance: {distance:.4f})")
                print(f"DEBUG: {chunk[:150]}...")
        else:
            print("DEBUG: NO RESULTS FROM VECTOR SEARCH!")

        context = "\n\n".join([row[0] for row in results])
        print(f"\nDEBUG: Final context length: {len(context)} characters")
        print(f"{'='*60}\n")

        return context

    except Exception as e:
        print(f"DEBUG ERROR in get_vector_context: {e}")
        import traceback
        traceback.print_exc()
        return ""

def ask_local_ai(user_question: str, retrieved_knowledge: str) -> str:
    """
    Calls Llama3 with context and returns a concise answer.
    """
    print(f"\n{'='*60}")
    print(f"DEBUG: CALLING LLM")
    print(f"{'='*60}")
    print(f"DEBUG: Question: {user_question}")
    print(f"DEBUG: Context length: {len(retrieved_knowledge)} chars")

    if not retrieved_knowledge:
        print("DEBUG: WARNING - Empty context!")
        return "No information available on this topic."

    print(f"DEBUG: Context preview: {retrieved_knowledge[:300]}...")

    try:
        ollama_client = ollama.Client(host='http://localhost:11434')

        system_instruction = RESPOND_TO_MESSAGE_SYSTEM_PROMPT.replace(
            "{{knowledge}}", retrieved_knowledge
        )

        print(f"DEBUG: Calling llama3...")
        response = ollama_client.chat(
            model=settings.chat_model,
            messages=[
                {'role': 'system', 'content': system_instruction},
                {'role': 'user', 'content': user_question},
            ],
            stream=False
        )

        answer = response['message']['content']
        print(f"DEBUG: LLM Response (original): {answer}")


        if len(answer) > settings.answer_max_length:
            # Find last complete sentence within limit
            truncated = answer[:settings.answer_max_length]

            # Find last period, question mark, or exclamation
            last_period = max(
                truncated.rfind('.'),
                truncated.rfind('!'),
                truncated.rfind('?')
            )

            if last_period > 50: #If we have at least one sentence
                answer = truncated[:last_period + 1]
            else:
                # No complete sentence, truncate with ellipsis
                answer = truncated[:settings.answer_max_length - 3] + "..."
        print(f"DEBUG: LLM Response (final): {answer} {len(answer)} characters")
        print(f"{'='*60}\n")
        return answer

    except Exception as e:
        print(f"DEBUG ERROR in ask_local_ai: {e}")
        import traceback
        traceback.print_exc()
        return f"Error: {str(e)}"

def answer_tax_question(question: str) -> dict:
    """
    Main function to answer tax questions using RAG.
    """
    print(f"\n{'#'*60}")
    print(f"DEBUG: NEW QUERY: {question}")
    print(f"{'#'*60}")

    try:
        # Step 1: Retrieve context
        context = get_vector_context(question)

        if not context.strip():
            print("DEBUG: No context retrieved!")
            return {
                "answer": "No relevant tax information found in the database.",
                "success": False,
                "chunks_found": 0
            }

        # Step 2: Generate answer
        answer = ask_local_ai(question, context)

        return {
            "answer": answer,
            "success": True,
            "chunks_found": settings.max_chunks
        }

    except Exception as e:
        print(f"DEBUG ERROR in answer_tax_question: {e}")
        import traceback
        traceback.print_exc()
        return {
            "answer": "Sorry, I encountered an error.",
            "success": False,
            "error": str(e),
            "chunks_found": 0
        }