Данный проект создаётся в качестве дипломной работы в университете. Коммиты могут иметь странное название и содержание так как данный репозиторий был создан из общего репозитория, где хранятся все учебные файлы. 

# HCMS (History Coffee Management System)

*Как запустить проект:*
1. В корневой папке проекта (где лежит файл docker-compose.yml) выполнить команду: `docker-compose up -d --build`
2. Приложение будет доступно в браузере по адресу: http://localhost:8080
3. Сервер базы данных будет проброшен на порт локольного компьютера 5432.

## Структура проекта

```
HCMSWeb/
├── src/
│   └── main/
│       ├── java/ru/mokrischev/vendingsupply/
│       │   ├── WebCurseWorkApplication.java          # Точка входа
│       │   │
│       │   ├── config/                               # Конфигурация
│       │   │   ├── SecurityConfig.java               # Настройки Spring Security и доступа по ролям
│       │   │   ├── WebConfig.java                    # Настройки веб (статика, загрузки)
│       │   │   ├── CustomAuthenticationSuccessHandler.java  # Перенаправление после логина
│       │   │   └── DataInitializer.java              # Начальные данные в БД
│       │   │
│       │   ├── controllers/                          # Веб-контроллеры (возвращают HTML)
│       │   │   ├── HomeController.java
│       │   │   ├── AuthController.java               # Регистрация / вход
│       │   │   ├── ProductController.java
│       │   │   ├── PublicController.java              # Публичные страницы
│       │   │   ├── AdminController.java
│       │   │   ├── AdminFeedbackController.java
│       │   │   ├── AdminFranchiseeController.java
│       │   │   ├── AdminOrderController.java
│       │   │   ├── AdminProductController.java
│       │   │   ├── AdminStatisticsController.java
│       │   │   ├── FranchiseeController.java
│       │   │   ├── FranchiseeEmployeeController.java
│       │   │   ├── FranchiseeMachineController.java
│       │   │   ├── FranchiseeOrderController.java
│       │   │   ├── FranchiseeServiceReportController.java
│       │   │   ├── FranchiseeStatisticsController.java
│       │   │   ├── FranchiseeWarehouseController.java
│       │   │   └── api/                              # REST API для мобильного приложения (JSON)
│       │   │       ├── MobileAuthController.java
│       │   │       └── MobileServiceController.java
│       │   │
│       │   ├── model/
│       │   │   ├── entity/                           # Сущности базы данных
│       │   │   │   ├── User.java
│       │   │   │   ├── Employee.java
│       │   │   │   ├── VendingMachine.java
│       │   │   │   ├── Product.java
│       │   │   │   ├── Order.java
│       │   │   │   ├── OrderItem.java
│       │   │   │   ├── OrderStatusLog.java
│       │   │   │   ├── Feedback.java
│       │   │   │   ├── FeedbackRequest.java
│       │   │   │   ├── ServiceReport.java
│       │   │   │   ├── ServiceReportConsumable.java
│       │   │   │   ├── ServiceReportPhoto.java
│       │   │   │   ├── StockMovement.java
│       │   │   │   └── WarehouseItem.java
│       │   │   └── enums/                            # Перечисления
│       │   │       ├── Role.java                     # ADMIN, FRANCHISEE
│       │   │       ├── OrderStatus.java
│       │   │       ├── FeedbackStatus.java
│       │   │       ├── OperationType.java
│       │   │       └── ScheduleType.java
│       │   │
│       │   ├── repository/                           # Интерфейсы для работы с БД (Spring Data JPA)
│       │   │   ├── UserRepository.java
│       │   │   ├── EmployeeRepository.java
│       │   │   ├── VendingMachineRepository.java
│       │   │   ├── ProductRepository.java
│       │   │   ├── OrderRepository.java
│       │   │   ├── OrderItemRepository.java
│       │   │   ├── OrderStatusLogRepository.java
│       │   │   ├── FeedbackRepository.java
│       │   │   ├── FeedbackRequestRepository.java
│       │   │   ├── ServiceReportRepository.java
│       │   │   ├── ServiceReportConsumableRepository.java
│       │   │   ├── ServiceReportPhotoRepository.java
│       │   │   ├── StockMovementRepository.java
│       │   │   └── WarehouseItemRepository.java
│       │   │
│       │   ├── services/                             # Бизнес-логика
│       │   │   ├── UserService.java
│       │   │   ├── CustomUserDetailsService.java
│       │   │   ├── EmployeeService.java
│       │   │   ├── VendingMachineService.java
│       │   │   ├── ProductService.java
│       │   │   ├── OrderService.java
│       │   │   └── WarehouseService.java
│       │   │
│       │   ├── security/                             # JWT для мобильного API
│       │   │   ├── JwtUtil.java
│       │   │   └── JwtAuthenticationFilter.java
│       │   │
│       │   ├── dto/                                  # Объекты для передачи данных
│       │   │   ├── RegistrationDTO.java
│       │   │   ├── ProductDTO.java
│       │   │   ├── CategoryDTO.java
│       │   │   └── BatchServiceForm.java
│       │   │
│       │   └── exceptions/
│       │       └── InsufficientStockException.java
│       │
│       └── resources/
│           ├── application.properties                # Настройки приложения
│           ├── static/
│           │   ├── css/                              # Стили
│           │   └── images/                           # Картинки
│           └── templates/                            # HTML-шаблоны Thymeleaf
│               ├── index.html                        # Главная страница
│               ├── login.html
│               ├── registration.html
│               ├── products.html
│               ├── contacts.html
│               ├── error.html
│               ├── admin-create-product.html
│               ├── fragments/                        # Переиспользуемые части (шапка, меню)
│               ├── admin/                            # Страницы админа
│               │   ├── dashboard.html
│               │   ├── statistics.html
│               │   ├── feedback/
│               │   ├── franchisee/
│               │   ├── orders/
│               │   └── products/
│               ├── franchisee/                       # Страницы франчайзи
│               │   ├── dashboard.html
│               │   ├── statistics.html
│               │   ├── warehouse.html
│               │   ├── employees/
│               │   ├── machines/
│               │   ├── orders/
│               │   ├── reports/
│               │   └── warehouse/
│               └── orders/
│
├── uploads/                                          # Загруженные фото (монтируется через Docker Volume)
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── mvnw / mvnw.cmd                                   # Maven Wrapper
```
