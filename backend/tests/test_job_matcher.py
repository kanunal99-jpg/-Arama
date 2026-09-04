from app.services.job_matcher import MatchInput, build_reasons, calculate_match_score, match_label


def test_calculate_match_score_uses_expected_weights():
    data = MatchInput(100, 80, 60, 40, 20)
    assert calculate_match_score(data) == 74.0


def test_score_is_clamped():
    data = MatchInput(120, -10, 50, 50, 50)
    assert calculate_match_score(data) == 58.0


def test_match_label_and_reasons():
    data = MatchInput(90, 85, 80, 40, 90)
    assert match_label(calculate_match_score(data)) == "güçlü"
    reasons = build_reasons(data, ["Excel"])
    assert "Yetenek uyumu" in reasons["strong_points"]
    assert "Maaş uyumu" in reasons["weak_points"]
    assert reasons["missing_skills"] == ["Excel"]
