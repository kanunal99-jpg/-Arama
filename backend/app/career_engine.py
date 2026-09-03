from dataclasses import dataclass


@dataclass(frozen=True)
class SkillAssessment:
    knowledge: float
    application: float
    scenario: float
    evidence: float

    def score(self) -> float:
        return round(
            self.knowledge * 0.25
            + self.application * 0.30
            + self.scenario * 0.25
            + self.evidence * 0.20,
            2,
        )


def gap_score(current_score: float, required_score: float) -> float:
    return round(max(required_score - current_score, 0.0), 2)


def priority_score(
    current_score: float,
    required_score: float,
    career_impact: float,
    developability: float,
) -> float:
    gap = gap_score(current_score, required_score) / 100
    return round(gap * (required_score / 100) * (career_impact / 100) * (developability / 100) * 100, 2)


def learning_route(score: float) -> str:
    if score >= 80:
        return "application"
    if score >= 60:
        return "teach-apply-test"
    return "fundamentals-example-guided-practice-independent-practice-test"
