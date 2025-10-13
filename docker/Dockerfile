FROM apache/spark:3.5.0-scala2.12-java11-python3-ubuntu

# Maintainer
LABEL maintainer="Gabriel Demetrios Lafis"
LABEL description="Distributed Data Processing Pipeline with Spark and Delta Lake"

USER root

# Install system dependencies
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    vim \
    git \
    && rm -rf /var/lib/apt/lists/*

# Install Python dependencies
COPY requirements.txt /tmp/
RUN pip3 install --no-cache-dir -r /tmp/requirements.txt

# Install SBT for Scala builds
RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list && \
    curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add && \
    apt-get update && \
    apt-get install -y sbt && \
    rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /opt/pipeline

# Copy project files
COPY . /opt/pipeline/

# Build Scala project
RUN sbt clean assembly

# Set environment variables
ENV SPARK_HOME=/opt/spark
ENV PATH=$PATH:$SPARK_HOME/bin:$SPARK_HOME/sbin
ENV PYTHONPATH=$SPARK_HOME/python:$SPARK_HOME/python/lib/py4j-0.10.9.7-src.zip

# Expose ports
EXPOSE 4040 8080 8081 7077

# Default command
CMD ["/bin/bash"]

