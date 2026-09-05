from typing import Any
import json

from openai import OpenAI

from app.core.config import settings


CAREER_ANALYSIS_SCHEMA = {
    "type": "object",
    "properties": {
        "summary": {"type": "string"},
        "role_fit_score": {"type": "number", "minimum": 0, "maximum": 100},
        "role_fit_reason": {"type": "string"},
        "strengths": {"type": "array", "items": {"type": "string"}},
        "skill_gaps": {"type": "array", "items": {"type": "string"}},
        "recommended_roles": {"type": "array", "items": {"type": "string"}},
        "actions": {"type": "array", "items": {"type": "string"}},
        "roadmap_30_60_90": {
            "type": "object",
            "properties": {
                "days_30": {"type": "array", "items": {"type": "string"}},
                "days_60": {"type": "array", "items": {"type": "string"}},
                "days_90": {"type": "array", "items": {"type": "string"}},
            },
            "required": ["days_30", "days_60", "days_90"],
            "additionalProperties": False,
        },
        "evidence": {"type": "array", "items": {"type": "string"}},
        "disclaimer": {"type": "string"},
    },
    "required": [
        "summary",
        "role_fit_score",
        "role_fit_reason",
        "strengths",
        "skill_gaps",
        "recommended_roles",
        "actions",
        "roadmap_30_60_90",
        "evidence",
        "disclaimer",
    ],
    "additionalProperties": False,
}

_MAX_PROFILE_JSON_CHARS = 30000


def _client() -> OpenAI:
    if not settings.openai_api_key:
        raise RuntimeError("OPENAI_API_KEY eksik")
    return OpenAI(api_key=settings.openai_api_key, timeout=30.0, max_retries=2)


def verify_openai_connection() -> dict[str, Any]:
    response = _client().chat.completions.create(
        model="gpt-4o-mini",
        temperature=0,
        max_tokens=8,
        messages=[
            {"role": "system", "content": "Return only the word OK."},
            {"role": "user", "content": "Connection test."},
        ],
    )
    content = (response.choices[0].message.content or "").strip()
    if not content:
        raise RuntimeError("OpenAI boş yanıt döndürdü")
    if content.upper() != "OK":
        raise RuntimeError("OpenAI beklenen sağlık yanıtını döndürmedi")
    return {"connected": True, "model": "gpt-4o-mini", "response": content}


def generate_career_analysis(target_role: str, profile: dict[str, Any]) -> dict[str, Any]:
    profile_json = json.dumps(profile, ensure_ascii=False, separators=(",", ":"))
    if len(profile_json) > _MAX_PROFILE_JSON_CHARS:
        raise ValueError("CV profili AI analiz sınırını aşıyor")

    target_role = target_role.strip()
    if len(target_role) < 2:
        raise ValueError("Hedef rol geçersiz")

    response = _client().chat.completions.create(
        model="gpt-4o-mini",
        temperature=0.2,
        response_format={
            "type": "json_schema",
            "json_schema": {
                "name": "career_analysis",
                "strict": True,
                "schema": CAREER_ANALYSIS_SCHEMA,
            },
        },
        messages=[
            {
                "role": "system",
                "content": (
                    "Sen ARAMA'nın AI Kariyer Analisti'sin. CV profilini hedef rolle karşılaştır. "
                    "Yalnızca verilen kanıtlara dayan, bilgi uydurma. Türkçe ve uygulanabilir analiz üret. "
                    "Güçlü yönler, beceri açıkları, alternatif roller ve 30/60/90 günlük yol haritası ver. "
                    "Skor rehber niteliğindedir; garanti değildir."
                ),
            },
            {
                "role": "user",
                "content": f"Hedef rol: {target_role}\nCV profili: {profile_json}",
            },
        ],
    )
    content = response.choices[0].message.content
    if not content:
        raise RuntimeError("OpenAI kariyer analizi boş yanıt döndürdü")
    return json.loads(content)
