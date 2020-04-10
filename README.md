# Simple-parser

Проект простого парсера базы вопросов и ответов для викторины

Для запуска надо добавить параметры БД в application.yml
```yml
spring:
  datasource:
    url: jdbc:postgresql://<ip>:<port>/<dbname>?currentSchema=<schema>
    username: <username>
    password: <password>
```
