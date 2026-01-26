#!/bin/bash
# 构建前端并复制到后端静态目录

echo "📦 构建前端..."
cd frontend
npm install && npm run build

echo "📋 复制到 src/main/resources/static/..."
cd ..
rm -rf src/main/resources/static/*
cp -r frontend/dist/* src/main/resources/static/

echo "✅ 完成！"
