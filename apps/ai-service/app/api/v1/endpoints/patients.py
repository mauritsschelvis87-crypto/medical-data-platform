from fastapi import APIRouter

router = APIRouter()

@router.get("/patients")
def get_patients():
    return [
        {"id": 1, "name": "Mock Patient"}
    ]