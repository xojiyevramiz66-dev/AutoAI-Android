# Как получить AutoAI.apk через GitHub Actions

## 1. Создай репозиторий
1. Войди на https://github.com
2. Нажми **New repository**
3. Назови его `AutoAI-Android`
4. Выбери **Public** или **Private**, затем **Create repository**

## 2. Загрузи файлы проекта
1. Распакуй `AutoAI-Android-CloudBuild.zip` на телефоне или компьютере.
2. В репозитории нажми **Add file → Upload files**.
3. Загрузи ВСЕ файлы и папки из распакованного проекта, включая скрытую папку `.github`.
4. Нажми **Commit changes**.

## 3. Запусти сборку
1. Открой вкладку **Actions** в репозитории.
2. Выбери workflow **Build AutoAI APK**.
3. Нажми **Run workflow → Run workflow**.
4. Дождись зелёной галочки.

## 4. Скачай APK
1. Открой завершённый запуск workflow.
2. Внизу страницы найди **Artifacts**.
3. Скачай `AutoAI-debug-apk`.
4. Распакуй архив артефакта и установи `app-debug.apk` на Android.

## Важно
- Это debug APK для тестирования, не подписанный релиз для Play Store.
- Текущая версия приложения требует указать LLM endpoint, API key и model в `LlmClient.kt`.
- Не публикуй API-ключ в открытом репозитории. Перед реальным использованием лучше вынести ключ на сервер.
- GitHub Actions запускает Gradle из workflow. Если сборка упадёт из-за версии Android Gradle Plugin/Gradle, открой вкладку Actions и пришли мне текст ошибки — подправлю проект.
