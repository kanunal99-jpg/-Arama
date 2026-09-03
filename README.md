# Arama — AI Career Platform

AI destekli kariyer, CV analizi ve iş eşleştirme platformu.

## Vizyon
CV'yi anlamak, yetenekleri çıkarmak, iş piyasasını taramak ve kullanıcıyı en uygun fırsatlarla açıklanabilir AI matching ile buluşturmak.

## Stack
- Backend: FastAPI / Python
- Frontend: Next.js 14 / TypeScript
- Database: PostgreSQL
- Vector search: Qdrant
- Queue/cache: Redis + Celery
- Crawling: Scrapy + Playwright
- AI/NLP: OpenAI + spaCy

## İlk sürüm
1. Authentication
2. CV upload/parse
3. Skill extraction
4. Job ingestion
5. Hybrid matching
6. Application tracking

## Repository structure
`backend/` API and domain services  
`frontend/` web application  
`crawler/` job source ingestion  
`infra/` local infrastructure  
`docs/` product and architecture documentation

> Not: API anahtarları, kullanıcı CV'leri ve production secret'ları repository'ye konulmaz.
