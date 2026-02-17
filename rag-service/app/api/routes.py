"""
FastAPI route handlers for the tax assistant API
"""
from fastapi import APIRouter, HTTPException
from app.models.schemas import QueryRequest, QueryResponse, HealthResponse
from app.services.ai_engine import answer_tax_question
from app.core.db import db
import ollama

router = APIRouter()

@router.post("/query", response_model=QueryResponse)
async def query_tax_assistant(request: QueryRequest):
    """
    Answer a Nigerian tax policy question using RAG.

    Args:
        request: QueryRequest containing the question

    Returns:
        QueryResponse with the answer and metadata

    Raises:
        HTTPException: If an error occurs during processing
    """
    try:
        # Call the AI engine to get the answer
        result = answer_tax_question(request.question)

        # Apply custom max_length if provided and smaller than default
        if request.max_length and request.max_length < 140:
            if len(result['answer']) > request.max_length:
                result['answer'] = result['answer'][:request.max_length - 3] + "..."

        return QueryResponse(**result)

    except Exception as e:
        print(f"API Error in query_tax_assistant: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/health", response_model=HealthResponse)
async def health_check():
    """
    Check the health status of the RAG service.
    Tests database and Ollama connectivity.

    Returns:
        HealthResponse with system status
    """
    # Check database connection
    try:
        db_status = "connected" if not db.is_closed() else "disconnected"
        if db.is_closed():
            db.connect()
            db_status = "connected"
    except Exception as e:
        print(f"Database health check failed: {e}")
        db_status = "disconnected"

    # Check Ollama connection
    try:
        ollama.list()  # Simple test to see if Ollama is running
        ollama_status = "connected"
    except Exception as e:
        print(f"Ollama health check failed: {e}")
        ollama_status = "disconnected"

    # Determine overall status
    overall_status = "healthy" if (db_status == "connected" and ollama_status == "connected") else "degraded"

    return HealthResponse(
        status=overall_status,
        database=db_status,
        ollama=ollama_status
    )