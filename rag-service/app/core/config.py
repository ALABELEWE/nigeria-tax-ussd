from pydantic_settings import BaseSettings
from functools import lru_cache
from typing import Optional
import os
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent.parent
ENV_FILE = BASE_DIR / ".env"

class Settings(BaseSettings):
    """Application settings loaded from environment variables"""

    # Database Configuration
    postgres_db_name: str
    postgres_db_host: str
    postgres_db_port: int
    postgres_db_user: str
    postgres_db_password: str

    # Ollama Configuration
    ollama_host: str
    embedding_model: str
    chat_model: str

    # API Configuration
    api_host: str
    api_port: int
    api_reload: bool

    # Performance Settings
    max_chunks: int
    answer_max_length: int

    class Config:
        env_file = ".env"
        case_sensitive = False

@lru_cache()
def get_settings() -> Settings:
    """Return cached settings instance"""
    return Settings()

# Optional: Print loaded settings for debugging (remove in production)
if __name__ == "__main__":
    settings = get_settings()
    print("=" * 50)
    print("Configuration loaded successfully!")
    print("=" * 50)
    print(f"Database: {settings.postgres_db_name}")
    print(f"Host: {settings.postgres_db_host}")
    print(f"Port: {settings.postgres_db_port}")
    print(f"Ollama: {settings.ollama_host}")
    print(f"API Port: {settings.api_port}")
    print("=" * 50)