from pydantic_settings import BaseSettings
from functools import lru_cache
import os
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent.parent
ENV_FILE = BASE_DIR / ".env"

class Settings(BaseSettings):
    """Application settings loaded from environment variables"""

    # Database Configuration (cloud-compatible)
    database_url: str = os.getenv("DATABASE_URL", "")
    postgres_db_name: str = os.getenv("POSTGRES_DB", "nigerian_tax")
    postgres_db_host: str = os.getenv("POSTGRES_HOST", "localhost")
    postgres_db_port: int = int(os.getenv("POSTGRES_PORT", "5432"))
    postgres_db_user: str = os.getenv("POSTGRES_USER", "postgres")
    postgres_db_password: str = os.getenv("POSTGRES_PASSWORD", "")

    # LLM Configuration
    use_groq: bool = os.getenv("USE_GROQ", "false").lower() == "true"
    groq_api_key: str = os.getenv("GROQ_API_KEY", "")

    # Ollama Configuration (for local development only)
    ollama_host: str = os.getenv("OLLAMA_HOST", "http://localhost:11434")

    # Model Configuration
    embedding_model: str = os.getenv("EMBEDDING_MODEL", "sentence-transformers/all-MiniLM-L6-v2")
    chat_model: str = os.getenv("CHAT_MODEL", "llama3-70b-8192")  # Groq model name

    # API Configuration
    api_host: str = os.getenv("API_HOST", "0.0.0.0")
    api_port: int = int(os.getenv("API_PORT", "8000"))
    api_reload: bool = os.getenv("API_RELOAD", "false").lower() == "true"

    # Performance Settings
    max_chunks: int = int(os.getenv("MAX_CHUNKS", "5"))
    answer_max_length: int = int(os.getenv("ANSWER_MAX_LENGTH", "130"))

    class Config:
        env_file = ".env"
        case_sensitive = False
        extra = "allow"

@lru_cache()
def get_settings() -> Settings:
    """Return cached settings instance"""
    return Settings()

# Helper to check if running in cloud
def is_cloud_environment() -> bool:
    """Detect if running in cloud (Render)"""
    return os.getenv("RENDER") is not None or os.getenv("DATABASE_URL") is not None