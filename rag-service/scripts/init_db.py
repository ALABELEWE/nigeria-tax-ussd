"""
Database initialization script
Run this to set up the database schema and verify connectivity
"""
import sys
from pathlib import Path

# Add parent directory to path so we can import app modules
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from app.core.db import initialize_db, db, Documents, DocumentInformationChunks
from app.core.config import get_settings

def main():
    """Initialize and verify database setup"""
    print("=" * 60)
    print("Nigerian Tax RAG Database Initialization")
    print("=" * 60)

    # Load settings
    settings = get_settings()
    print(f"\n Configuration:")
    print(f"   Database: {settings.postgres_db_name}")
    print(f"   Host: {settings.postgres_db_host}:{settings.postgres_db_port}")
    print(f"   User: {settings.postgres_db_user}")

    # Initialize database
    print(f"\n Initializing database...")
    success = initialize_db()

    if not success:
        print("\n Database initialization failed!")
        return False

    # Verify connection
    print(f"\n Testing database connection...")
    try:
        # Test query
        doc_count = Documents.select().count()
        chunk_count = DocumentInformationChunks.select().count()

        print(f"\n Database Statistics:")
        print(f"   Documents: {doc_count}")
        print(f"   Chunks: {chunk_count}")

        # Test pgvector extension
        result = db.execute_sql("SELECT extname FROM pg_extension WHERE extname = 'vector';")
        has_pgvector = result.fetchone() is not None

        print(f"\n🔌 Extensions:")
        print(f"   pgvector: {' Installed' if has_pgvector else 'Not installed'}")

        # Check HNSW index
        index_check = db.execute_sql("""
                                     SELECT indexname
                                     FROM pg_indexes
                                     WHERE tablename = 'document_information_chunks'
                                       AND indexname = 'embedding_idx';
                                     """)
        has_index = index_check.fetchone() is not None
        print(f"   HNSW Index: {'Created' if has_index else 'Not created'}")

        print("\n" + "=" * 60)
        print("Database is ready for use!")
        print("=" * 60)

        return True

    except Exception as e:
        print(f"\n Database verification failed: {e}")
        return False

    finally:
        if not db.is_closed():
            db.close()

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)