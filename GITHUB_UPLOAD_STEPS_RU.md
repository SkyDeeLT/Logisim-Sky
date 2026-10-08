# Как выложить Logisim Sky на GitHub

## 1. Создай пустой репозиторий

На GitHub нажми **New repository** и укажи:

- Repository name: `Logisim-Sky`
- Description: `A modern desktop-focused derivative of classic Logisim 2.7.1`
- Visibility: `Public`

Не добавляй README, `.gitignore` и License на сайте: они уже есть в подготовленной папке.

## 2. Распакуй подготовленный архив

Распакуй `Logisim-Sky-GitHub-v1.0.0.zip` в обычную папку, например:

```text
C:\Projects\Logisim-Sky
```

## 3. Открой терминал в этой папке

В проводнике Windows зайди в папку проекта, нажми правой кнопкой по пустому месту и открой Terminal / PowerShell / Git Bash.

## 4. Выполни команды Git

Замени `YOUR_GITHUB_NAME` на свой ник GitHub:

```bash
git init
git add .
git commit -m "Initial release: Logisim Sky v1.0.0"
git branch -M main
git remote add origin https://github.com/YOUR_GITHUB_NAME/Logisim-Sky.git
git push -u origin main
```

Если Git попросит войти в GitHub, пройди авторизацию через браузер.

## 5. Создай Release v1.0.0

Открой репозиторий на GitHub:

**Releases -> Create a new release**

Укажи:

- Tag: `v1.0.0`
- Release title: `Logisim Sky v1.0.0`

В описание вставь текст из `RELEASE_NOTES_v1.0.0.md`.

## 6. Прикрепи файлы релиза

Добавь в Assets:

- `Logisim-Sky-1.0-Apple-Silicon.zip`
- `Logisim-Sky-1.0.jar`
- `Logisim-Sky-1.0-Windows-x64.exe`

Нажми **Publish release**.

## 7. Что НЕ надо заливать в код репозитория

Не добавляй готовые `.exe`, `.zip`, `.dmg`, `.app` и локальные JAR-зависимости в основной Git-репозиторий. Готовые сборки лучше держать в GitHub Releases.
