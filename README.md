# 📊 Distributed Data Processing Pipeline

[![Python](https://img.shields.io/badge/Python-3.12-blue.svg)](https://www.python.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED.svg)](https://www.docker.com/)
[![Prometheus](https://img.shields.io/badge/Prometheus-2.48-E6522C.svg)](https://prometheus.io/)
[![Apache Spark](https://img.shields.io/badge/Apache Spark-3.5-E25A1C.svg)](https://spark.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[English](#english) | [Português](#português)

---

## English

### 🎯 Overview

**Distributed Data Processing Pipeline** — Enterprise-grade distributed data processing pipeline with Apache Spark (Scala + Python), Delta Lake, and Airflow orchestration

Total source lines: **4,707** across **28** files in **3** languages.

### ✨ Key Features

- **Production-Ready Architecture**: Modular, well-documented, and following best practices
- **Comprehensive Implementation**: Complete solution with all core functionality
- **Clean Code**: Type-safe, well-tested, and maintainable codebase
- **Easy Deployment**: Docker support for quick setup and deployment

### 🚀 Quick Start

#### Prerequisites
- Python 3.12+
- Docker and Docker Compose (optional)

#### Installation

1. **Clone the repository**
```bash
git clone https://github.com/galafis/distributed-data-processing-pipeline.git
cd distributed-data-processing-pipeline
```

2. **Create virtual environment**
```bash
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
```

3. **Install dependencies**
```bash
pip install -r requirements.txt
```




## 🐳 Docker

```bash
# Build and start
docker-compose up -d

# View logs
docker-compose logs -f

# Stop
docker-compose down
```

### 🧪 Testing

```bash
# Run all tests
pytest

# Run with coverage
pytest --cov --cov-report=html

# Run with verbose output
pytest -v
```

### 📁 Project Structure

```
distributed-data-processing-pipeline/
├── config/
│   └── pipeline.yaml
├── dags/
│   └── data_pipeline_dag.py
├── data/
│   ├── processed/
│   ├── raw/
│   └── staging/
├── docker/
├── notebooks/
├── project/
├── scripts/
│   ├── generate_quality_report.py
│   ├── run_integration_tests.sh
│   ├── run_performance_tests.sh
│   └── validate_project.sh
├── src/
│   ├── main/
│   │   ├── python/
│   │   └── scala/
│   └── test/
│       └── scala/
├── tests/
│   └── python/
│       ├── integration/
│       ├── unit/
│       └── __init__.py
├── AUDIT_FINAL_REPORT.md
├── AUDIT_SUMMARY.md
├── CONTRIBUTING.md
├── DOCUMENTATION.md
├── README.md
└── docker-compose.yml
```

### 🛠️ Tech Stack

| Technology | Usage |
|------------|-------|
| Python | 13 files |
| Scala | 12 files |
| Shell | 3 files |

### 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

### 👤 Author

**Gabriel Demetrios Lafis**

- GitHub: [@galafis](https://github.com/galafis)
- LinkedIn: [Gabriel Demetrios Lafis](https://linkedin.com/in/gabriel-demetrios-lafis)

---

## Português

### 🎯 Visão Geral

**Distributed Data Processing Pipeline** — Enterprise-grade distributed data processing pipeline with Apache Spark (Scala + Python), Delta Lake, and Airflow orchestration

Total de linhas de código: **4,707** em **28** arquivos em **3** linguagens.

### ✨ Funcionalidades Principais

- **Arquitetura Pronta para Produção**: Modular, bem documentada e seguindo boas práticas
- **Implementação Completa**: Solução completa com todas as funcionalidades principais
- **Código Limpo**: Type-safe, bem testado e manutenível
- **Fácil Implantação**: Suporte Docker para configuração e implantação rápidas

### 🚀 Início Rápido

#### Pré-requisitos
- Python 3.12+
- Docker e Docker Compose (opcional)

#### Instalação

1. **Clone the repository**
```bash
git clone https://github.com/galafis/distributed-data-processing-pipeline.git
cd distributed-data-processing-pipeline
```

2. **Create virtual environment**
```bash
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
```

3. **Install dependencies**
```bash
pip install -r requirements.txt
```




### 🧪 Testes

```bash
# Run all tests
pytest

# Run with coverage
pytest --cov --cov-report=html

# Run with verbose output
pytest -v
```

### 📁 Estrutura do Projeto

```
distributed-data-processing-pipeline/
├── config/
│   └── pipeline.yaml
├── dags/
│   └── data_pipeline_dag.py
├── data/
│   ├── processed/
│   ├── raw/
│   └── staging/
├── docker/
├── notebooks/
├── project/
├── scripts/
│   ├── generate_quality_report.py
│   ├── run_integration_tests.sh
│   ├── run_performance_tests.sh
│   └── validate_project.sh
├── src/
│   ├── main/
│   │   ├── python/
│   │   └── scala/
│   └── test/
│       └── scala/
├── tests/
│   └── python/
│       ├── integration/
│       ├── unit/
│       └── __init__.py
├── AUDIT_FINAL_REPORT.md
├── AUDIT_SUMMARY.md
├── CONTRIBUTING.md
├── DOCUMENTATION.md
├── README.md
└── docker-compose.yml
```

### 🛠️ Stack Tecnológica

| Tecnologia | Uso |
|------------|-----|
| Python | 13 files |
| Scala | 12 files |
| Shell | 3 files |

### 📄 Licença

Este projeto está licenciado sob a Licença MIT - veja o arquivo [LICENSE](LICENSE) para detalhes.

### 👤 Autor

**Gabriel Demetrios Lafis**

- GitHub: [@galafis](https://github.com/galafis)
- LinkedIn: [Gabriel Demetrios Lafis](https://linkedin.com/in/gabriel-demetrios-lafis)
