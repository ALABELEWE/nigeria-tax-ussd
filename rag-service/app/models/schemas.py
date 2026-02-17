"""
Pydantic schemas for API request and response validation
"""
from pydantic import BaseModel, Field
from typing import Optional

class QueryRequest(BaseModel):
    """Request model for tax question queries"""
    question: str = Field(
        ...,
        min_length=1,
        max_length=500,
        description="Nigerian tax policy question",
        examples=["What is the current VAT rate in Nigeria?"]
    )
    max_length: Optional[int] = Field(
        140,
        ge=50,
        le=500,
        description="Maximum answer length in characters"
    )

class QueryResponse(BaseModel):
    """Response model for tax question answers"""
    answer: str = Field(..., description="AI-generated answer to the question")
    success: bool = Field(..., description="Whether the query was successful")
    chunks_found: int = Field(0, description="Number of relevant document chunks found")
    error: Optional[str] = Field(None, description="Error message if query failed")

class HealthResponse(BaseModel):
    """Health check response"""
    status: str = Field(..., description="Overall system status")
    database: str = Field(..., description="Database connection status")
    ollama: str = Field(..., description="Ollama service status")