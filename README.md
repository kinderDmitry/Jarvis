# JARVIS — GitHub ZIP Importer

Этот архив предназначен для первого запуска в новом GitHub-репозитории.

## Как использовать

1. Создай **пустой** GitHub repository.
2. Загрузи в него содержимое этого архива: папку `.github/` и папку `input/`.
3. В `input/` уже находится `JARVIS_Home_Multitool_v4_LOCKSCREEN.zip`.
4. GitHub автоматически запустит `Unpack JARVIS Android Project` после загрузки ZIP.
5. Workflow распакует Android-проект в корень репозитория, проверит наличие `settings.gradle(.kts)`, `build.gradle(.kts)` и `app`, затем создаст commit.
6. После завершения в корне репозитория будет обычный Android-проект JARVIS.

Если автоматический запуск не произошёл, открой **Actions → Unpack JARVIS Android Project → Run workflow**.

## Важно

Workflow требует `contents: write`, потому что ему нужно сделать commit распакованного проекта обратно в репозиторий.
