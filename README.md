# Jarvis Android MVP (source)

Это минимальный Android Studio проект (Kotlin), реализующий Jarvis-like MVP:
- Push-to-talk button (hold to speak) using Android SpeechRecognizer
- Sends recognized text to a configurable backend: POST /ask with JSON {"text":"..."}
- Receives JSON {"answer":"..."} and speaks it with Android TextToSpeech

## Как собрать APK
1. Скачай и открой проект в Android Studio (Arctic Fox или новее).
2. Задай в `MainActivity.kt` переменную `BACKEND_URL` на адрес сервера с маршрутом `/ask`.
   Пример: `http://192.168.1.100:5005/ask` — сервер можно поднять на ПК, использовав Flask из предыдущего Python MVP.
3. Синхронизируй проект и построй APK (Build -> Build Bundle(s) / APK(s) -> Build APK(s)).

## Серверная часть (быстро)
Используй Flask endpoint:
```
from flask import Flask, request, jsonify
app = Flask(__name__)
@app.post('/ask')
def ask():
    q = request.json.get('text','')
    # возвращай ответ в JSON:
    return jsonify({'answer': 'Пример ответа: ' + q})
app.run(host='0.0.0.0', port=5005)
```

## Примечания безопасности
- Приложение не содержит встроенной LLM — это клиент. LLM вызовы выполняет сервер, где ты разместишь OpenAI/ollama ключи.
- Проверь CORS/безопасность, если будешь выставлять сервер в интернет.

Если хочешь — я могу:
- автоматически подготовить GitHub Actions workflow, чтобы при пуше собирать APK;
- добавить встроенный UI для настройки адреса бэкенда;
- добавить offline TTS/voice improvements.


## Новые возможности в обновлённой версии (Android-only)

- В приложении теперь есть экран настроек (доступ: долгий тап по строке состояния 'Jarvis — готов').
- Можно задать `backend_url` или включить локальный mock (`Use mock`) — тогда приложение будет работать полностью автономно без сервера.
- Mock-режим понимает простые фразы: 'кто ты', 'время', 'запомни ...' и отвечает локально.
- Для интеграции с реальным LLM поднять на ПК простой Flask endpoint, описанный ранее.


## Автоматическая сборка и публикация в GitHub Releases

Проект содержит GitHub Actions workflow `/.github/workflows/android-release.yml`.
Когда ты:
1. Создашь новый репозиторий на GitHub и запушишь в него содержимое этого проекта (ветка `main`),
2. Actions автоматически запустится и соберёт `debug` и `release` APK,
3. APK будут добавлены в новый Release (тег `apk-release-<run_id>`).

Пример команд:
```
git init
git add .
git commit -m "Initial Jarvis Android"
git branch -M main
git remote add origin https://github.com/<твое-имя>/<repo>.git
git push -u origin main
```

> **Примечание про подпись release APK**: workflow помещает `app-release-unsigned.apk` в релиз. Чтобы подписать release автоматически, можно:
> - добавить секреты с ключом подписи и командой, которая подпишет apk (keystore в `SECRET_KEYSTORE_BASE64`, пароль в `SECRET_KEYSTORE_PASSWORD`), и расширить workflow шаги `jarsigner`/`apksigner`.
> Я могу помочь добавить автоподпись, если хочешь — просто дай знать.
