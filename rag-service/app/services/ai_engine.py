"""
AI engine for RAG retrieval and answer generation
Supports both Ollama (local) and Groq (cloud)
"""
from app.core.config import get_settings, is_cloud_environment
from app.core.constants import RESPOND_TO_MESSAGE_SYSTEM_PROMPT
from app.core.db import db
from sentence_transformers import SentenceTransformer

settings = get_settings()

# Initialize embedding model (works both local and cloud)
embedding_model = SentenceTransformer(settings.embedding_model)

def get_embeddings(text: str) -> list:
    """Generate embeddings using sentence-transformers (cloud-compatible)"""
    return embedding_model.encode(text).tolist()

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
        print("DEBUG: Generating query embedding...")
        query_embedding = get_embeddings(query_text)
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

def ask_groq(user_question: str, retrieved_knowledge: str) -> str:
    """Call Groq API (cloud-compatible)"""
    from groq import Groq

    print(f"\n{'='*60}")
    print(f"DEBUG: CALLING GROQ API")
    print(f"{'='*60}")

    try:
        client = Groq(api_key=settings.groq_api_key)

        system_instruction = RESPOND_TO_MESSAGE_SYSTEM_PROMPT.replace(
            "{{knowledge}}", retrieved_knowledge
        )

        print(f"DEBUG: Calling Groq with model: {settings.chat_model}")
        response = client.chat.completions.create(
            model=settings.chat_model,
            messages=[
                {"role": "system", "content": system_instruction},
                {"role": "user", "content": user_question}
            ],
            temperature=0.3,
            max_tokens=200
        )

        answer = response.choices[0].message.content
        print(f"DEBUG: Groq Response: {answer}")

        # Truncate if needed
        if len(answer) > settings.answer_max_length:
            truncated = answer[:settings.answer_max_length]
            last_period = max(
                truncated.rfind('.'),
                truncated.rfind('!'),
                truncated.rfind('?')
            )
            if last_period > 50:
                answer = truncated[:last_period + 1]
            else:
                answer = truncated[:settings.answer_max_length - 3] + "..."

        print(f"DEBUG: Final answer: {len(answer)} characters")
        print(f"{'='*60}\n")
        return answer

    except Exception as e:
        print(f"DEBUG ERROR in ask_groq: {e}")
        import traceback
        traceback.print_exc()
        return f"Error: {str(e)}"

def ask_ollama(user_question: str, retrieved_knowledge: str) -> str:
    """Call Ollama (local only)"""
    import ollama

    print(f"\n{'='*60}")
    print(f"DEBUG: CALLING OLLAMA (LOCAL)")
    print(f"{'='*60}")

    try:
        ollama_client = ollama.Client(host=settings.ollama_host)

        system_instruction = RESPOND_TO_MESSAGE_SYSTEM_PROMPT.replace(
            "{{knowledge}}", retrieved_knowledge
        )

        print(f"DEBUG: Calling Ollama with model: {settings.chat_model}")
        response = ollama_client.chat(
            model=settings.chat_model,
            messages=[
                {'role': 'system', 'content': system_instruction},
                {'role': 'user', 'content': user_question},
            ],
            stream=False
        )

        answer = response['message']['content']
        print(f"DEBUG: Ollama Response: {answer}")

        # Truncate if needed
        if len(answer) > settings.answer_max_length:
            truncated = answer[:settings.answer_max_length]
            last_period = max(
                truncated.rfind('.'),
                truncated.rfind('!'),
                truncated.rfind('?')
            )
            if last_period > 50:
                answer = truncated[:last_period + 1]
            else:
                answer = truncated[:settings.answer_max_length - 3] + "..."

        print(f"DEBUG: Final answer: {len(answer)} characters")
        print(f"{'='*60}\n")
        return answer

    except Exception as e:
        print(f"DEBUG ERROR in ask_ollama: {e}")
        import traceback
        traceback.print_exc()
        return f"Error: {str(e)}"

def ask_local_ai(user_question: str, retrieved_knowledge: str) -> str:
    """
    Smart routing: Use Groq in cloud, Ollama locally
    """
    if not retrieved_knowledge:
        print("DEBUG: WARNING - Empty context!")
        return "No information available on this topic."

    # Auto-detect environment
    if settings.use_groq or is_cloud_environment():
        return ask_groq(user_question, retrieved_knowledge)
    else:
        return ask_ollama(user_question, retrieved_knowledge)

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