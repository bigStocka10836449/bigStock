FROM openjdk:22-ea-21-jdk-slim-bullseye

# 以 root 用户运行以下命令
USER root

# 添加一个名为bigstock的用户
RUN useradd -m -s /bin/bash bigstock

# 授予bigstock用户sudo权限
RUN echo "bigstock ALL=(ALL) NOPASSWD: ALL" >> /etc/sudoers
# 创建目录并设置权限
RUN mkdir -p /opt/bigstock/logs && \
    chown -R bigstock:bigstock /opt/bigstock

# Workspace path
WORKDIR /home/bigstock

# 将bigstockmodule复制到bigstock文件夹下
COPY ./bigstock-schedule.jar /opt/bigstock/
COPY ./init.sh /opt/bigstock/

# 確保 init.sh 有執行權限
RUN chmod +x /opt/bigstock/init.sh

# 切换到bigstock用户
USER bigstock
# 执行init.sh脚本
CMD ["bash", "/opt/bigstock/init.sh"]
# 讓容器保持運行狀態
#CMD ["sleep", "infinity"]
