from pydantic import BaseModel, Field
from typing import List

class CodeComment (BaseModel):
    file_path: str = Field (description="The exact file path from the repository")
    line_number: int = Field (description="The specific line number in the modified file where the issue exists")
    body: str = Field (description="The actionable markdown comment for the developer")

class ReviewResult (BaseModel):
    comments: List[CodeComment] = Field (description="List of inline code comments for the PR")