# ARAMA

**AI Career Intelligence Platform**

> Kariyerinin bir sonraki adımını bul.

Arama; CV analizi, yetenek çıkarımı, iş ilanı keşfi, hibrit AI eşleştirme ve başvuru takibini tek bir deneyimde birleştiren web + Android odaklı kariyer platformudur.

## Vision

LinkedIn ölçeğinde profesyonel ağ mantığını; AI kariyer koçluğu, iş arama, maaş istihbaratı ve başvuru yönetimiyle birleştirmek.

## Product pillars

- CV → structured profile → skills
- Multi-source job discovery
- Hybrid semantic + skill matching
- Explainable match scores
- Application tracking
- Salary intelligence
- AI cover letters and interview preparation
- Career path recommendations
- GitHub/profile analysis
- Fraud and job-risk signals
- HR/talent intelligence

## Technology

- Frontend: Next.js 14 + TypeScript + Tailwind
- Backend: FastAPI + Python
- Database: PostgreSQL
- Vector search: Qdrant
- Cache/queue: Redis + Celery
- Crawling: Scrapy + Playwright
- AI/NLP: OpenAI + spaCy
- Deployment: Docker + CI/CD

## Design direction

Professional, trustworthy and modern. Primary visual language uses deep navy with electric cyan accents, supported by neutral surfaces and high-contrast typography. The web and future Android app share the same design system.

## Repository structure

```text
backend/          FastAPI API and domain services
frontend/         Next.js web application
crawler/          job source crawlers and normalization
ai/               prompts, embeddings and evaluations
infrastructure/   Docker, reverse proxy and monitoring
docs/             product and technical documentation
```

## Development principles

1. Build the core loop first: CV → skills → jobs → match → application.
2. Keep AI explainable; never expose an unexplained score as fact.
3. Treat personal data as sensitive and design for KVKK/GDPR principles.
4. Respect source terms, robots directives and ethical scraping practices.
5. Keep web and Android experiences aligned through a shared design system.
6. Test every major domain before production release.

## Status

🚧 Active development — Foundation phase.

<!-- android-release-trigger: 2026-09-03 -->
