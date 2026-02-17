"""
Utility functions for the RAG service
"""
from typing import Callable, Iterable, TypeVar, Union
import json

IterableItemType = TypeVar('IterableItemType')

def find(pred: Callable[[IterableItemType], bool], iterable: Iterable[IterableItemType]) -> Union[IterableItemType, None]:
    """
    Finds the first item in an iterable that matches the predicate.

    Args:
        pred: Predicate function to test each element
        iterable: Iterable to search

    Returns:
        First matching element or None
    """
    return next((element for element in iterable if pred(element)), None)

def clean_json_response(raw_text: str) -> str:
    """
    Extracts the JSON block from a string.
    Handles cases where the LLM adds text before or after the JSON.

    Args:
        raw_text: Raw text that may contain JSON

    Returns:
        Cleaned JSON string or original text if no valid JSON found
    """
    try:
        start = raw_text.find('{')
        end = raw_text.rfind('}') + 1

        if start != -1 and end != 0:
            json_str = raw_text[start:end]
            json.loads(json_str)  # Validate JSON
            return json_str

        return raw_text
    except json.JSONDecodeError:
        return raw_text