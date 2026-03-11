#!/bin/bash

# 1. 基礎目錄初始化
mkdir -p /persist/xrdp /persist/python_libs
chown -R testuser:testuser /persist

# 2. 安裝系統依賴與語系
apt-get update
apt-get install -y locales python3-tk python3-dev gnome-screenshot libgl1-mesa-glx libglib2.0-0 libgomp1
apt-get install -y thunar-archive-plugin p7zip-full

# 設定中文化環境 (解決 CSV 亂碼)
locale-gen zh_TW.UTF-8
update-locale LANG=zh_TW.UTF-8

# 3. 建立虛擬 Windows 路徑
mkdir -p "/C:/Program Files/Google/Chrome/Application/"
ln -sf /usr/bin/google-chrome "/C:/Program Files/Google/Chrome/Application/chrome.exe"

# 4. RDP 與桌面環境持久化
echo "xfce4-session" > /persist/.xsession
ln -sf /persist/.xsession /home/testuser/.xsession

# 5. 持久化 xrdp 配置
if [ ! -f /persist/xrdp/xrdp.ini ]; then
    cp -r /etc/xrdp/* /persist/xrdp/
fi
rm -rf /etc/xrdp
ln -sf /persist/xrdp /etc/xrdp

# 6. 強制寫入變數到 .bashrc
BASHRC="/home/testuser/.bashrc"
if ! grep -q "python_libs" "$BASHRC"; then
    echo "export LANG=zh_TW.UTF-8" >> "$BASHRC"
    echo "export LC_ALL=zh_TW.UTF-8" >> "$BASHRC"
    echo "export DISPLAY=:10.0" >> "$BASHRC"
    echo "export PYTHONPATH=\"\${PYTHONPATH}:/persist/python_libs\"" >> "$BASHRC"
    chown testuser:testuser "$BASHRC"
fi

# 7. 設定當前環境變數
export LANG=zh_TW.UTF-8
export LC_ALL=zh_TW.UTF-8
export DISPLAY=:10.0
export PYTHONPATH="${PYTHONPATH}:/persist/python_libs"

# 8. 啟動服務
service dbus start
sudo -u testuser xfconf-query -c xfwm4 -p /general/use_compositing -s false || true

xrdp-sesman
exec xrdp --nodaemon
