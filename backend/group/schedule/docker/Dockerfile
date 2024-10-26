FROM openjdk:22-ea-21-jdk-slim-bullseye

# 以 root 用户运行以下命令
USER root

# 添加一个名为bigstock的用户
RUN useradd -m -s /bin/bash bigstock

# 授予bigstock用户sudo权限
RUN echo "bigstock ALL=(ALL) NOPASSWD: ALL" >> /etc/sudoers
# 创建目录并设置权限
RUN mkdir -p /opt/bigstock/logs && \
    mkdir -p /opt/bigstock/download && \ 
    chown -R bigstock:bigstock /opt/bigstock
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


# 安装chrome-for-testing版本
RUN wget -q -O /tmp/chrome-for-testing.zip https://storage.googleapis.com/chrome-for-testing-public/126.0.6478.61/linux64/chrome-linux64.zip \
    && unzip /tmp/chrome-for-testing.zip -d /usr/local/share/chrome \
    && ln -s /usr/local/share/chrome/chrome-linux64/chrome /usr/local/bin/google-chrome \
    && rm /tmp/chrome-for-testing.zip

# 安装与chrome-for-testing版本匹配的ChromeDriver
RUN wget -q -O /tmp/chromedriver.zip https://storage.googleapis.com/chrome-for-testing-public/126.0.6478.61/linux64/chromedriver-linux64.zip \
    && unzip /tmp/chromedriver.zip -d /usr/local/bin/ \
    && mv /usr/local/bin/chromedriver-linux64/chromedriver /usr/local/bin/chromedriver \
    && rm -rf /tmp/chromedriver.zip /usr/local/bin/chromedriver-linux64


# 设置ChromeDriver的权限
RUN chmod +x /usr/local/bin/chromedriver

# Workspace path
WORKDIR /home/bigstock

# 将bigstockmodule复制到bigstock文件夹下
COPY ./bigstock-schedule.jar /opt/bigstock/
COPY ./google-api-cloud-speech-to-text.json /opt/bigstock/google-api-cloud-speech-to-text.json
COPY ./init.sh /opt/bigstock/

# 確保 init.sh 有執行權限
RUN chmod +x /opt/bigstock/init.sh

# 切换到bigstock用户
USER bigstock
# 执行init.sh脚本
CMD ["bash", "/opt/bigstock/init.sh"]
# 讓容器保持運行狀態
#CMD ["sleep", "infinity"]