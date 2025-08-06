Library Management System (Java + PostgreSQL + Swing)
This is a desktop-based Library Management System built with Java Swing and backed by a PostgreSQL database. 
It allows users (primarily students or staff) to borrow books, manage library inventory, 
delete unavailable books, and view developer information, all within a user-friendly graphical interface.

 Features
 Book Borrowing System
Input student name, registration number, book title, and author

Checks if the book is available before issuing

Automatically generates a borrowing transaction with:

Borrow date
Expected return date (7 days later)
Borrowing time
Updates the book's availability status
Saves borrower details and transaction logs in the database

 Library Inventory Management
Add new books to the system
Delete books that are no longer available for borrowing
View a list of all books currently in the library
Deleted books are archived in a deleted_books table

 Transaction Logging
Each borrow action is saved in a transactions table
Transactions include:
Borrower's name and registration number
Book title and author
Date borrowed and return date

 About Section
Displays information about the developer (name, reg number, department, etc.)
