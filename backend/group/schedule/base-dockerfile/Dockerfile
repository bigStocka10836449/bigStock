# Base image，包含 Java、爬蟲相關套件、Chrome、ChromeDriver
FROM openjdk:22-ea-21-jdk-slim-bullseye

# 以 root 用户运行以下命令
USER root

# 添加 bigstock 用戶
RUN useradd -m -s /bin/bash bigstock

# 授予 bigstock sudo 權限
RUN echo "bigstock ALL=(ALL) NOPASSWD: ALL" >> /etc/sudoers

# 建立必要目錄並修改權限
RUN mkdir -p /opt/bigstock/logs && \
    mkdir -p /opt/bigstock/download && \
    chown -R bigstock:bigstock /opt/bigstock

# 安裝必要的工具
RUN apt-get update && apt-get install -y \
    wget \
    unzip \
    curl \
    gnupg \
    gnupg2 \
    gnupg1 \
    xvfb \
    libxi6 \
    libgconf-2-4 \
    libglu1-mesa \
    libxrender1 \
    libxtst6 \
    libxss1 \
    libgtk-3-0 \
    software-properties-common \
    libnss3 \
    libgconf-2-4 \
    libxi6 \
    libxcursor1 \
    libxdamage1 \
    libxrandr2 \
    libxcomposite1 \
    libasound2 \
    libpangocairo-1.0-0 \
    libatk1.0-0 \
    libatk-bridge2.0-0 \
    libgtk-3-0 \
    libgbm1 \
    libglib2.0-0 \
    ffmpeg

# 安裝 Chrome
RUN wget -q -O /tmp/chrome-for-testing.zip https://storage.googleapis.com/chrome-for-testing-public/126.0.6478.61/linux64/chrome-linux64.zip \
    && unzip /tmp/chrome-for-testing.zip -d /usr/local/share/chrome \
    && ln -s /usr/local/share/chrome/chrome-linux64/chrome /usr/local/bin/google-chrome \
    && rm /tmp/chrome-for-testing.zip

# 安裝 ChromeDriver
RUN wget -q -O /tmp/chromedriver.zip https://storage.googleapis.com/chrome-for-testing-public/126.0.6478.61/linux64/chromedriver-linux64.zip \
    && unzip /tmp/chromedriver.zip -d /usr/local/bin/ \
    && mv /usr/local/bin/chromedriver-linux64/chromedriver /usr/local/bin/chromedriver \
    && rm -rf /tmp/chromedriver.zip /usr/local/bin/chromedriver-linux64

# 設定 ChromeDriver 權限
RUN chmod +x /usr/local/bin/chromedriver

# 設置工作目錄
WORKDIR /home/bigstock

# 切換用戶
USER bigstock
