# 繼承 Base Image
FROM isaac0410/bstockbaseschedule:v1.0

# 設置工作目錄
WORKDIR /opt/bigstock

# 複製應用程式
COPY ./bigstock-schedule.jar /opt/bigstock/
COPY ./init.sh /opt/bigstock/
COPY ./google-api-cloud-speech-to-text.json /opt/bigstock/google-api-cloud-speech-to-text.json

# 確保 init.sh 可執行
RUN chmod +x /opt/bigstock/init.sh

# 切換用戶
USER bigstock

# 預設執行指令
CMD ["bash", "/opt/bigstock/init.sh"]
