"""
Database models and connection using Peewee ORM with pgvector
"""
from pgvector.peewee import VectorField
from peewee import PostgresqlDatabase, Model, TextField, ForeignKeyField, AutoField
from app.core.config import get_settings

settings = get_settings()

# Database connection
db = PostgresqlDatabase(
    settings.postgres_db_name,
    host=settings.postgres_db_host,
    port=settings.postgres_db_port,
    user=settings.postgres_db_user,
    password=settings.postgres_db_password,
)

# Models (matching your existing database schema)
class Documents(Model):
    id = AutoField()
    name = TextField()

    class Meta:
        database = db
        db_table = 'documents'

class Tags(Model):
    id = AutoField()
    name = TextField()

    class Meta:
        database = db
        db_table = 'tags'

class DocumentTags(Model):
    id = AutoField()
    document_id = ForeignKeyField(Documents, backref="document_tags", on_delete='CASCADE')
    tag_id = ForeignKeyField(Tags, backref="document_tags", on_delete='CASCADE')

    class Meta:
        database = db
        db_table = 'document_tags'

class DocumentInformationChunks(Model):
    id = AutoField()
    document_id = ForeignKeyField(Documents, backref="document_information_chunks", on_delete='CASCADE')
    chunk = TextField()
    embedding = VectorField(dimensions=384)

    class Meta:
        database = db
        db_table = 'document_information_chunks'

def initialize_db():
    """Initialize database and create tables with pgvector extension"""
    try:
        if db.is_closed():
            db.connect()

        print("🔌 Connecting to database...")

        # Enable pgvector extension
        db.execute_sql('CREATE EXTENSION IF NOT EXISTS vector;')
        print("✅ pgvector extension enabled")

        # Create tables (only if they don't exist)
        db.create_tables([Documents, Tags, DocumentTags, DocumentInformationChunks], safe=True)
        print("✅ Database tables verified")

        # Create HNSW index for fast vector search
        db.execute_sql('''
                       CREATE INDEX IF NOT EXISTS embedding_idx
                           ON document_information_chunks
                               USING hnsw (embedding vector_cosine_ops)
                           WITH (m=16, ef_construction=64);
                       ''')
        print("✅ HNSW index created/verified")

        print("✅ Database initialization complete")
        return True

    except Exception as e:
        print(f"❌ Database initialization error: {e}")
        return False

def close_db():
    """Close database connection gracefully"""
    if not db.is_closed():
        db.close()
        print("🔌 Database connection closed")