"""
Script to upload PDF documents to the RAG database
Usage: python scripts/upload_document.py path/to/document.pdf
"""
import sys
from pathlib import Path

# Add parent directory to path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from app.core.db import db, Documents, DocumentInformationChunks
from app.core.config import get_settings
import pdfplumber
from langchain_text_splitters import RecursiveCharacterTextSplitter
from peewee import chunked
from sentence_transformers import SentenceTransformer

settings = get_settings()

# Initialize embedding model (same as ai_engine.py)
print("Loading embedding model...")
embedding_model = SentenceTransformer(settings.embedding_model)
print(f"Loaded model: {settings.embedding_model}")

# Initialize text splitter
text_splitter = RecursiveCharacterTextSplitter(
    chunk_size=1000,
    chunk_overlap=100,
    separators=["\n\n", "\n", ".", " ", ""]
)

def extract_text_from_pdf(pdf_path):
    """Extract text from PDF file"""
    text = ""
    with pdfplumber.open(pdf_path) as pdf:
        for page in pdf.pages:
            page_text = page.extract_text()
            if page_text:
                text += page_text + "\n\n"
    return text

def upload_document(pdf_path, doc_name=None):
    """Upload a PDF document to the database"""

    if doc_name is None:
        doc_name = Path(pdf_path).stem

    print(f"\nProcessing: {doc_name}")
    print("=" * 60)

    # Extract text
    print("Extracting text from PDF...")
    raw_text = extract_text_from_pdf(pdf_path)

    if not raw_text.strip():
        print("No text could be extracted from this PDF")
        return False

    print(f"Extracted {len(raw_text)} characters")

    # Split into chunks
    print("Splitting into chunks...")
    chunks = text_splitter.split_text(raw_text)
    valid_chunks = [c for c in chunks if c.strip()]
    print(f"Created {len(valid_chunks)} chunks")

    # Generate embeddings
    print("Generating embeddings...")
    embeddings = []

    for i, chunk in enumerate(valid_chunks, 1):
        print(f"   Processing chunk {i}/{len(valid_chunks)}...", end='\r')

        try:
            # Use sentence-transformers (cloud-compatible)
            embedding = embedding_model.encode(chunk).tolist()
            embeddings.append(embedding)
        except Exception as e:
            print(f"\nError generating embedding for chunk {i}: {e}")
            return False

    print(f"\nGenerated {len(embeddings)} embeddings")

    # Save to database
    print("Saving to database...")
    try:
        with db.atomic():
            # Create document
            doc = Documents.create(name=doc_name)

            # Prepare chunk data
            chunk_data = [
                {
                    'document_id': doc.id,
                    'chunk': chunk_text,
                    'embedding': emb
                }
                for chunk_text, emb in zip(valid_chunks, embeddings)
            ]

            # Insert in batches
            for batch in chunked(chunk_data, 100):
                DocumentInformationChunks.insert_many(batch).execute()

        print(f"Successfully uploaded '{doc_name}' with {len(valid_chunks)} chunks")
        print("=" * 60)
        return True

    except Exception as e:
        print(f"Database error: {e}")
        import traceback
        traceback.print_exc()
        return False

def main():
    if len(sys.argv) < 2:
        print("Usage: python scripts/upload_document.py <path_to_pdf>")
        print("Example: python scripts/upload_document.py data/NIGERIA_TAX_ACT_2025.pdf")
        sys.exit(1)

    pdf_path = sys.argv[1]

    if not Path(pdf_path).exists():
        print(f"File not found: {pdf_path}")
        sys.exit(1)

    # Connect to database
    print("Connecting to database...")
    print(f"Host: {settings.postgres_db_host}")
    print(f"Database: {settings.postgres_db_name}")

    success = upload_document(pdf_path)

    # Close database connection
    if not db.is_closed():
        db.close()
        print("Database connection closed")

    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main()