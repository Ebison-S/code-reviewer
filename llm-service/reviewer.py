import os
import json
from dotenv import load_dotenv
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from schemas import ReviewResult

load_dotenv ()

# Initialize the model with structured output
llm = ChatOpenAI (model=os.getenv ("OPENAI_MODEL"), temperature=0.1, api_key=os.getenv ("OPENAI_API_KEY"))
structured_reviewer = llm.with_structured_output (ReviewResult)

prompt = ChatPromptTemplate.from_messages ([
    ("system", """You are an expert AI code reviewer.
    Analyze the provided Git diff. Focus ONLY on:
    1. Logic errors, race conditions, or edge cases.
    2. Security vulnerabilities.
    3. Missing edge-case tests.
    
    DO NOT comment on code formatting, style, or syntax (assume linters handle this).
    If a file looks good, do not generate a comment for it."""),
    ("human", "Pull Request Title: {pr_title}\n\nGit Diff:\n{git_diff}")
])

review_chain = prompt | structured_reviewer

def generate_review (pr_title: str, git_diff: str) -> str:
    """Executes the LangChain review and returns a JSON string."""
    result: ReviewResult = review_chain.invoke ({"pr_title": pr_title, "git_diff": git_diff})
    
    if isinstance(result, dict):
        return json.dumps(result)
    return result.model_dump_json ()