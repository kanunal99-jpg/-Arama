from dataclasses import dataclass
from typing import Iterable


@dataclass(frozen=True)
class MatchInput:
    skill_fit: float
    experience_fit: float
    location_fit: float
    salary_fit: float
    career_goal_fit: float


def _clamp(value: float) -> float:
    return max(0.0, min(100.0, float(value)))


def calculate_match_score(data: MatchInput) -> float:
    """Deterministic, explainable job-match score."""
    weights = {
        "skill_fit": 0.40,
        "experience_fit": 0.25,
        "location_fit": 0.10,
        "salary_fit": 0.10,
        "career_goal_fit": 0.15,
    }
    return round(sum(_clamp(getattr(data, key)) * weight for key, weight in weights.items()), 1)


def match_label(score: float) -> str:
    score = _clamp(score)
    if score >= 85:
        return "çok güçlü"
    if score >= 70:
        return "güçlü"
    if score >= 55:
        return "uygun"
    if score >= 40:
        return "geliştirilebilir"
    return "zayıf"


def build_reasons(data: MatchInput, missing_skills: Iterable[str] = ()) -> dict:
    dimensions = [
        ("skill_fit", "Yetenek uyumu", data.skill_fit),
        ("experience_fit", "Deneyim uyumu", data.experience_fit),
        ("location_fit", "Lokasyon uyumu", data.location_fit),
        ("salary_fit", "Maaş uyumu", data.salary_fit),
        ("career_goal_fit", "Kariyer hedefi uyumu", data.career_goal_fit),
    ]
    strong = [label for _, label, value in dimensions if value >= 75]
    weak = [label for _, label, value in dimensions if value < 50]
    return {
        "strong_points": strong,
        "weak_points": weak,
        "missing_skills": list(missing_skills),
    }
