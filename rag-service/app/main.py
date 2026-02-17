"""
FastAPI application entry point for Nigerian Tax RAG Service
"""
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
from app.core.db import initialize_db, close_db
from app.core.config import get_settings
from app.api.routes import router

settings = get_settings()

@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Lifespan context manager for startup and shutdown events
    """
    # Startup
    print("=" * 50)
    print("Starting Nigerian Tax RAG Service...")
    print("=" * 50)
    initialize_db()
    print("Service ready to accept requests")
    print("=" * 50)

    yield

    # Shutdown
    print("=" * 50)
    print("Shutting down Nigerian Tax RAG Service...")
    close_db()
    print("Service stopped gracefully")
    print("=" * 50)

# Create FastAPI application
app = FastAPI(
    title="Nigerian Tax RAG API",
    description="RAG-powered API for answering Nigerian tax policy questions via USSD/SMS",
    version="1.0.0",
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url="/redoc"
)

# CORS middleware - allow Spring Boot backend to call this API
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:8080",  # Spring Boot development
        "http://127.0.0.1:8080",
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include API routes with prefix
app.include_router(router, prefix="/api/v1", tags=["tax-assistant"])

@app.get("/")
async def root():
    """Root endpoint with service information"""
    return {
        "service": "Nigerian Tax RAG API",
        "version": "1.0.0",
        "status": "running",
        "docs": "/docs",
        "health": "/api/v1/health"
    }

# Run with: uvicorn app.main:app --reload --port 8000
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host=settings.api_host,
        port=settings.api_port,
        reload=settings.api_reload
    )