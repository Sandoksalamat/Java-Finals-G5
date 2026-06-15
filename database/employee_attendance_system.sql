-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Jun 15, 2026 at 06:12 PM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `employee_attendance_system`
--

-- --------------------------------------------------------

--
-- Table structure for table `announcements`
--

CREATE TABLE `announcements` (
  `id` int(11) NOT NULL,
  `title` varchar(150) NOT NULL,
  `content` text NOT NULL,
  `target_audience` enum('ALL','ADMIN','EMPLOYEE') DEFAULT 'ALL',
  `posted_by` int(11) DEFAULT NULL,
  `posted_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `valid_until` date DEFAULT NULL,
  `status` enum('ACTIVE','INACTIVE') DEFAULT 'ACTIVE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `announcements`
--

INSERT INTO `announcements` (`id`, `title`, `content`, `target_audience`, `posted_by`, `posted_at`, `valid_until`, `status`) VALUES
(1, 'Attendance Reminder', 'Please clock in and clock out using the assigned attendance station.', 'ALL', 1, '2026-06-01 13:57:45', '2026-12-31', 'ACTIVE');

--
-- Triggers `announcements`
--
DELIMITER $$
CREATE TRIGGER `trg_announcement_notice` AFTER INSERT ON `announcements` FOR EACH ROW BEGIN
 INSERT INTO notifications(user_id,title,message_body,notification_type)
 SELECT id,NEW.title,NEW.content,'ANNOUNCEMENT' FROM users WHERE status='ACTIVE' AND (NEW.target_audience='ALL' OR role=NEW.target_audience);
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Table structure for table `attendance_corrections`
--

CREATE TABLE `attendance_corrections` (
  `id` int(11) NOT NULL,
  `employee_id` int(11) NOT NULL,
  `attendance_id` int(11) DEFAULT NULL,
  `correction_date` date NOT NULL,
  `requested_clock_in` datetime DEFAULT NULL,
  `requested_clock_out` datetime DEFAULT NULL,
  `reason` varchar(255) NOT NULL,
  `status` enum('PENDING','APPROVED','REJECTED') DEFAULT 'PENDING',
  `reviewed_by` int(11) DEFAULT NULL,
  `reviewed_at` datetime DEFAULT NULL,
  `admin_remarks` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Triggers `attendance_corrections`
--
DELIMITER $$
CREATE TRIGGER `trg_correction_reviewed` AFTER UPDATE ON `attendance_corrections` FOR EACH ROW BEGIN
 IF NEW.status <> OLD.status THEN
  INSERT INTO notifications(user_id,title,message_body,notification_type)
  SELECT user_id,'Attendance Correction Updated',CONCAT('Your attendance correction #',NEW.id,' status is ',NEW.status,'.'),'CORRECTION' FROM employees WHERE id=NEW.employee_id AND user_id IS NOT NULL;
 END IF;
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Table structure for table `attendance_locations`
--

CREATE TABLE `attendance_locations` (
  `id` int(11) NOT NULL,
  `location_code` varchar(20) NOT NULL,
  `location_name` varchar(120) NOT NULL,
  `address` varchar(255) DEFAULT NULL,
  `allowed_ip` varchar(80) DEFAULT NULL,
  `status` enum('ACTIVE','INACTIVE') DEFAULT 'ACTIVE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `attendance_locations`
--

INSERT INTO `attendance_locations` (`id`, `location_code`, `location_name`, `address`, `allowed_ip`, `status`) VALUES
(1, 'MAIN', 'Main Office Attendance Station', 'Main Building Lobby', '192.168.1.%', 'ACTIVE'),
(2, 'ITLAB', 'IT Office Station', 'Second Floor', '192.168.2.%', 'ACTIVE');

-- --------------------------------------------------------

--
-- Table structure for table `attendance_logs`
--

CREATE TABLE `attendance_logs` (
  `id` int(11) NOT NULL,
  `attendance_id` int(11) DEFAULT NULL,
  `employee_id` int(11) NOT NULL,
  `log_type` enum('TIME_IN','TIME_OUT','ADJUSTMENT') NOT NULL,
  `logged_at` datetime NOT NULL,
  `device_id` int(11) DEFAULT NULL,
  `notes` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `attendance_logs`
--

INSERT INTO `attendance_logs` (`id`, `attendance_id`, `employee_id`, `log_type`, `logged_at`, `device_id`, `notes`) VALUES
(1, 1, 1, 'TIME_IN', '2026-06-09 20:25:51', NULL, ''),
(2, 1, 1, 'TIME_OUT', '2026-06-09 20:27:02', NULL, ''),
(3, 4, 1, 'TIME_IN', '2026-06-10 23:19:56', NULL, ''),
(4, 4, 1, 'TIME_OUT', '2026-06-10 23:20:04', NULL, ''),
(5, 5, 1, 'TIME_IN', '2026-06-11 15:05:57', NULL, '');

-- --------------------------------------------------------

--
-- Table structure for table `attendance_records`
--

CREATE TABLE `attendance_records` (
  `id` int(11) NOT NULL,
  `employee_id` int(11) NOT NULL,
  `attendance_date` date NOT NULL,
  `shift_id` int(11) DEFAULT NULL,
  `location_id` int(11) DEFAULT NULL,
  `clock_in` datetime DEFAULT NULL,
  `clock_out` datetime DEFAULT NULL,
  `work_minutes` int(11) DEFAULT 0,
  `late_minutes` int(11) DEFAULT 0,
  `undertime_minutes` int(11) DEFAULT 0,
  `overtime_minutes` int(11) DEFAULT 0,
  `attendance_status` enum('PRESENT','LATE','ABSENT','ON_LEAVE','REST_DAY','HOLIDAY','CORRECTED') DEFAULT 'PRESENT',
  `source` enum('MANUAL','BIOMETRIC','RFID','QR') DEFAULT 'MANUAL',
  `remarks` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `attendance_records`
--

INSERT INTO `attendance_records` (`id`, `employee_id`, `attendance_date`, `shift_id`, `location_id`, `clock_in`, `clock_out`, `work_minutes`, `late_minutes`, `undertime_minutes`, `overtime_minutes`, `attendance_status`, `source`, `remarks`) VALUES
(1, 1, '2026-06-09', 1, 1, '2026-06-09 20:25:51', '2026-06-09 20:27:02', 0, 735, 0, 207, 'LATE', 'MANUAL', ''),
(4, 1, '2026-06-10', 1, 1, '2026-06-10 23:19:56', '2026-06-10 23:20:04', 0, 909, 0, 380, 'LATE', 'MANUAL', ''),
(5, 1, '2026-06-11', 1, 1, '2026-06-11 15:05:57', NULL, 0, 415, 0, 0, 'LATE', 'MANUAL', '');

-- --------------------------------------------------------

--
-- Table structure for table `attendance_summaries`
--

CREATE TABLE `attendance_summaries` (
  `id` int(11) NOT NULL,
  `payroll_period_id` int(11) NOT NULL,
  `employee_id` int(11) NOT NULL,
  `present_days` decimal(5,2) DEFAULT 0.00,
  `absent_days` decimal(5,2) DEFAULT 0.00,
  `leave_days` decimal(5,2) DEFAULT 0.00,
  `late_minutes` int(11) DEFAULT 0,
  `undertime_minutes` int(11) DEFAULT 0,
  `overtime_hours` decimal(7,2) DEFAULT 0.00,
  `generated_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `audit_logs`
--

CREATE TABLE `audit_logs` (
  `id` int(11) NOT NULL,
  `user_id` int(11) DEFAULT NULL,
  `action_type` varchar(50) NOT NULL,
  `module_name` varchar(80) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `logged_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `audit_logs`
--

INSERT INTO `audit_logs` (`id`, `user_id`, `action_type`, `module_name`, `description`, `logged_at`) VALUES
(1, 1, 'LOGIN', 'AUTHENTICATION', 'Logged into attendance system.', '2026-06-01 13:58:02'),
(2, 1, 'LOGOUT', 'AUTHENTICATION', 'Administrator logged out.', '2026-06-01 14:01:19'),
(3, 2, 'LOGIN', 'AUTHENTICATION', 'Logged into attendance system.', '2026-06-01 14:01:27'),
(4, 2, 'UPDATE', 'PROFILE', 'Updated employee profile.', '2026-06-01 14:01:40'),
(5, 2, 'LOGIN', 'AUTHENTICATION', 'Logged into attendance system.', '2026-06-01 14:36:52'),
(6, 2, 'LOGOUT', 'AUTHENTICATION', 'Employee logged out.', '2026-06-01 14:37:03'),
(7, 2, 'LOGIN', 'AUTHENTICATION', 'Logged into attendance system.', '2026-06-01 14:41:39'),
(8, 2, 'LOGIN', 'AUTHENTICATION', 'Logged into attendance system.', '2026-06-01 14:53:36'),
(9, 2, 'LOGIN', 'AUTHENTICATION', 'Logged into attendance system.', '2026-06-01 14:55:54'),
(10, 2, 'LOGIN', 'AUTHENTICATION', 'Logged into attendance system.', '2026-06-01 15:01:33'),
(11, 2, 'LOGIN', 'AUTHENTICATION', 'Logged into attendance system.', '2026-06-01 15:38:30'),
(12, 2, 'LOGIN', 'AUTHENTICATION', 'Logged into attendance system.', '2026-06-01 15:39:00'),
(13, 2, 'LOGIN', 'AUTHENTICATION', 'Logged into attendance system.', '2026-06-01 16:06:21'),
(14, 2, 'LOGIN', 'AUTHENTICATION', 'Logged into attendance system.', '2026-06-01 16:07:41'),
(15, 2, 'LOGIN', 'AUTHENTICATION', 'Logged into attendance system.', '2026-06-01 16:10:46'),
(16, 2, 'LOGIN', 'AUTHENTICATION', 'Logged into attendance system.', '2026-06-01 16:19:34'),
(17, 2, 'LOGIN', 'AUTHENTICATION', 'Logged into attendance system.', '2026-06-01 16:21:45'),
(18, 2, 'LOGIN', 'AUTHENTICATION', 'Logged into attendance system.', '2026-06-01 16:24:04'),
(19, 2, 'LOGIN', 'AUTHENTICATION', 'Logged into attendance system.', '2026-06-02 01:49:23'),
(20, 2, 'LOGIN', 'AUTHENTICATION', 'Logged into attendance system.', '2026-06-02 01:51:24'),
(21, 2, 'LOGIN', 'AUTHENTICATION', 'Logged into attendance system.', '2026-06-09 12:25:35'),
(22, 2, 'TIME_IN', 'ATTENDANCE', 'Employee clocked in.', '2026-06-09 12:25:51'),
(23, 2, 'TIME_OUT', 'ATTENDANCE', 'Employee clocked out.', '2026-06-09 12:27:02'),
(24, 2, 'UPDATE', 'PROFILE', 'Updated employee profile.', '2026-06-09 12:28:02'),
(25, 2, 'LOGIN', 'AUTHENTICATION', 'Logged into attendance system.', '2026-06-09 12:35:25'),
(26, 2, 'LOGIN', 'AUTHENTICATION', 'Logged into attendance system.', '2026-06-09 12:42:50'),
(27, 2, 'UPDATE', 'PROFILE', 'Updated employee profile.', '2026-06-09 12:46:42'),
(28, 2, 'UPDATE', 'PROFILE', 'Updated employee profile.', '2026-06-09 12:51:54'),
(29, 2, 'UPDATE', 'PROFILE', 'Updated employee profile.', '2026-06-09 12:52:01'),
(30, 2, 'UPDATE', 'PROFILE', 'Updated employee profile.', '2026-06-09 13:11:13'),
(31, 2, 'UPDATE', 'PROFILE', 'Updated employee profile.', '2026-06-09 13:11:20'),
(32, 2, 'LOGIN', 'AUTHENTICATION', 'Logged into attendance system.', '2026-06-09 14:01:06'),
(33, 2, 'LOGIN', 'AUTHENTICATION', 'Logged into attendance system.', '2026-06-10 15:19:36'),
(34, 2, 'LOGOUT', 'AUTHENTICATION', 'Employee logged out.', '2026-06-10 15:19:39'),
(35, 2, 'LOGIN', 'AUTHENTICATION', 'Logged into attendance system.', '2026-06-10 15:19:50'),
(36, 2, 'TIME_IN', 'ATTENDANCE', 'Employee clocked in.', '2026-06-10 15:19:56'),
(37, 2, 'TIME_OUT', 'ATTENDANCE', 'Employee clocked out.', '2026-06-10 15:20:04'),
(38, 2, 'LOGOUT', 'AUTHENTICATION', 'Employee logged out.', '2026-06-10 15:20:36'),
(39, 2, 'LOGIN', 'AUTHENTICATION', 'Logged into attendance system.', '2026-06-10 15:28:08'),
(40, 2, 'LOGIN', 'AUTHENTICATION', 'Logged into attendance system.', '2026-06-10 15:38:21'),
(41, 2, 'LOGIN', 'AUTHENTICATION', 'Logged into attendance system.', '2026-06-10 15:52:08'),
(42, 2, 'UPDATE', 'MEDICAL', 'Updated employee medical details.', '2026-06-10 15:52:22'),
(43, 2, 'LOGOUT', 'AUTHENTICATION', 'Employee logged out.', '2026-06-10 15:54:21'),
(44, 2, 'LOGIN', 'AUTHENTICATION', 'Logged into attendance system.', '2026-06-10 15:54:26'),
(45, 2, 'LOGIN', 'AUTHENTICATION', 'Logged into attendance system.', '2026-06-10 15:58:16'),
(46, 2, 'LOGIN', 'AUTHENTICATION', 'Logged into attendance system.', '2026-06-10 16:01:50'),
(47, 2, 'LOGIN', 'AUTHENTICATION', 'Logged into attendance system.', '2026-06-11 02:30:16'),
(48, 2, 'LOGIN', 'AUTHENTICATION', 'Logged into attendance system.', '2026-06-11 07:05:46'),
(49, 2, 'TIME_IN', 'ATTENDANCE', 'Employee clocked in.', '2026-06-11 07:05:57'),
(50, 2, 'LOGIN', 'AUTHENTICATION', 'Logged into attendance system.', '2026-06-15 15:36:23'),
(51, 2, 'LOGIN', 'AUTHENTICATION', 'Logged into attendance system.', '2026-06-15 15:46:12'),
(52, 2, 'SUBMIT', 'SICK_RECORDS', 'Filed sick record.', '2026-06-15 15:46:43'),
(53, 2, 'LOGOUT', 'AUTHENTICATION', 'Employee logged out.', '2026-06-15 15:46:46'),
(54, 1, 'LOGIN', 'AUTHENTICATION', 'Logged into attendance system.', '2026-06-15 15:46:51'),
(55, 1, 'UPDATE', 'SICK_MONITORING', 'Updated sick record #1', '2026-06-15 15:47:19'),
(56, 2, 'LOGIN', 'AUTHENTICATION', 'Logged into attendance system.', '2026-06-15 16:09:25');

-- --------------------------------------------------------

--
-- Table structure for table `biometric_devices`
--

CREATE TABLE `biometric_devices` (
  `id` int(11) NOT NULL,
  `location_id` int(11) DEFAULT NULL,
  `device_code` varchar(30) NOT NULL,
  `device_name` varchar(100) DEFAULT NULL,
  `device_type` enum('MANUAL','BIOMETRIC','RFID','QR') DEFAULT 'MANUAL',
  `ip_address` varchar(60) DEFAULT NULL,
  `status` enum('ONLINE','OFFLINE','MAINTENANCE') DEFAULT 'ONLINE',
  `last_sync_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `biometric_devices`
--

INSERT INTO `biometric_devices` (`id`, `location_id`, `device_code`, `device_name`, `device_type`, `ip_address`, `status`, `last_sync_at`) VALUES
(1, 1, 'DEV-MANUAL-01', 'Manual Desktop Login', 'MANUAL', NULL, 'ONLINE', NULL);

-- --------------------------------------------------------

--
-- Table structure for table `departments`
--

CREATE TABLE `departments` (
  `id` int(11) NOT NULL,
  `department_code` varchar(20) NOT NULL,
  `department_name` varchar(100) NOT NULL,
  `manager_name` varchar(120) DEFAULT NULL,
  `location` varchar(120) DEFAULT NULL,
  `status` enum('ACTIVE','INACTIVE') DEFAULT 'ACTIVE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `departments`
--

INSERT INTO `departments` (`id`, `department_code`, `department_name`, `manager_name`, `location`, `status`) VALUES
(1, 'HR', 'Human Resources', 'Maria Santos', 'Main Office', 'ACTIVE'),
(2, 'IT', 'Information Technology', 'Carlo Reyes', '2nd Floor', 'ACTIVE'),
(3, 'ACC', 'Accounting', 'Liza Cruz', 'Main Office', 'ACTIVE');

-- --------------------------------------------------------

--
-- Table structure for table `employees`
--

CREATE TABLE `employees` (
  `id` int(11) NOT NULL,
  `user_id` int(11) DEFAULT NULL,
  `employee_no` varchar(30) NOT NULL,
  `department_id` int(11) DEFAULT NULL,
  `position_id` int(11) DEFAULT NULL,
  `date_hired` date DEFAULT NULL,
  `birth_date` date DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  `emergency_contact` varchar(150) DEFAULT NULL,
  `employment_status` enum('ACTIVE','ON_LEAVE','RESIGNED','TERMINATED') DEFAULT 'ACTIVE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `employees`
--

INSERT INTO `employees` (`id`, `user_id`, `employee_no`, `department_id`, `position_id`, `date_hired`, `birth_date`, `address`, `emergency_contact`, `employment_status`) VALUES
(1, 2, 'EMP-0001', 2, 2, '2025-06-01', NULL, 'Manila, Philippines', 'Ana Dela Cruz - 09171234567', 'ACTIVE');

-- --------------------------------------------------------

--
-- Table structure for table `employee_medical_details`
--

CREATE TABLE `employee_medical_details` (
  `id` int(11) NOT NULL,
  `employee_id` int(11) NOT NULL,
  `blood_type` enum('A+','A-','AB+','AB-','B+','B-','O+','O-') DEFAULT NULL,
  `allergy` varchar(255) DEFAULT NULL,
  `existing_condition` varchar(255) DEFAULT NULL,
  `emergency_notes` text DEFAULT NULL,
  `medical_certificate` varchar(500) DEFAULT NULL,
  `workplace_injury_report` varchar(500) DEFAULT NULL,
  `health_declaration` varchar(500) DEFAULT NULL,
  `fitness_to_work_clearance` varchar(500) DEFAULT NULL,
  `wellness_activity` varchar(500) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `employee_medical_details`
--

INSERT INTO `employee_medical_details` (`id`, `employee_id`, `blood_type`, `allergy`, `existing_condition`, `emergency_notes`, `medical_certificate`, `workplace_injury_report`, `health_declaration`, `fitness_to_work_clearance`, `wellness_activity`, `created_at`, `updated_at`) VALUES
(1, 1, 'AB+', 'Cancer', 'Cancer', 'Cancer', NULL, NULL, NULL, NULL, NULL, '2026-06-10 15:52:22', '2026-06-10 15:52:22');

-- --------------------------------------------------------

--
-- Table structure for table `holidays`
--

CREATE TABLE `holidays` (
  `id` int(11) NOT NULL,
  `holiday_date` date NOT NULL,
  `holiday_name` varchar(120) NOT NULL,
  `holiday_type` enum('REGULAR','SPECIAL_NON_WORKING','COMPANY') DEFAULT 'REGULAR',
  `multiplier` decimal(4,2) DEFAULT 1.00,
  `status` enum('ACTIVE','INACTIVE') DEFAULT 'ACTIVE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `holidays`
--

INSERT INTO `holidays` (`id`, `holiday_date`, `holiday_name`, `holiday_type`, `multiplier`, `status`) VALUES
(1, '2026-06-12', 'Independence Day', 'REGULAR', 2.00, 'ACTIVE');

-- --------------------------------------------------------

--
-- Table structure for table `leave_requests`
--

CREATE TABLE `leave_requests` (
  `id` int(11) NOT NULL,
  `employee_id` int(11) NOT NULL,
  `leave_type_id` int(11) NOT NULL,
  `start_date` date NOT NULL,
  `end_date` date NOT NULL,
  `total_days` decimal(5,2) NOT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `status` enum('PENDING','APPROVED','REJECTED','CANCELLED') DEFAULT 'PENDING',
  `filed_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `reviewed_by` int(11) DEFAULT NULL,
  `reviewed_at` datetime DEFAULT NULL,
  `admin_remarks` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Triggers `leave_requests`
--
DELIMITER $$
CREATE TRIGGER `trg_leave_created` AFTER INSERT ON `leave_requests` FOR EACH ROW BEGIN
 INSERT INTO notifications(user_id,title,message_body,notification_type)
 SELECT user_id,'Leave Request Filed',CONCAT('Your leave request #',NEW.id,' is pending approval.'),'LEAVE' FROM employees WHERE id=NEW.employee_id AND user_id IS NOT NULL;
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_leave_reviewed` AFTER UPDATE ON `leave_requests` FOR EACH ROW BEGIN
 IF NEW.status <> OLD.status THEN
  INSERT INTO notifications(user_id,title,message_body,notification_type)
  SELECT user_id,'Leave Request Updated',CONCAT('Your leave request #',NEW.id,' status is ',NEW.status,'.'),'LEAVE' FROM employees WHERE id=NEW.employee_id AND user_id IS NOT NULL;
 END IF;
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Table structure for table `leave_types`
--

CREATE TABLE `leave_types` (
  `id` int(11) NOT NULL,
  `leave_code` varchar(20) NOT NULL,
  `leave_name` varchar(100) NOT NULL,
  `annual_credits` decimal(5,2) DEFAULT 0.00,
  `paid_status` enum('PAID','UNPAID') DEFAULT 'PAID',
  `status` enum('ACTIVE','INACTIVE') DEFAULT 'ACTIVE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `leave_types`
--

INSERT INTO `leave_types` (`id`, `leave_code`, `leave_name`, `annual_credits`, `paid_status`, `status`) VALUES
(1, 'VL', 'Vacation Leave', 15.00, 'PAID', 'ACTIVE'),
(2, 'SL', 'Sick Leave', 15.00, 'PAID', 'ACTIVE'),
(3, 'EL', 'Emergency Leave', 5.00, 'PAID', 'ACTIVE'),
(4, 'LWOP', 'Leave Without Pay', 0.00, 'UNPAID', 'ACTIVE');

-- --------------------------------------------------------

--
-- Table structure for table `messages`
--

CREATE TABLE `messages` (
  `id` int(11) NOT NULL,
  `sender_id` int(11) NOT NULL,
  `receiver_id` int(11) NOT NULL,
  `subject` varchar(150) NOT NULL,
  `message_body` text NOT NULL,
  `sent_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `read_status` enum('UNREAD','READ') DEFAULT 'UNREAD'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Triggers `messages`
--
DELIMITER $$
CREATE TRIGGER `trg_message_notice` AFTER INSERT ON `messages` FOR EACH ROW BEGIN
 INSERT INTO notifications(user_id,title,message_body,notification_type) VALUES (NEW.receiver_id,CONCAT('New Message: ',NEW.subject),NEW.message_body,'MESSAGE');
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Table structure for table `notifications`
--

CREATE TABLE `notifications` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `title` varchar(150) NOT NULL,
  `message_body` text NOT NULL,
  `notification_type` varchar(40) DEFAULT 'SYSTEM',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `read_status` enum('UNREAD','READ') DEFAULT 'UNREAD'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `overtime_requests`
--

CREATE TABLE `overtime_requests` (
  `id` int(11) NOT NULL,
  `employee_id` int(11) NOT NULL,
  `overtime_date` date NOT NULL,
  `requested_hours` decimal(5,2) NOT NULL,
  `purpose` varchar(255) DEFAULT NULL,
  `status` enum('PENDING','APPROVED','REJECTED','CANCELLED') DEFAULT 'PENDING',
  `approved_hours` decimal(5,2) DEFAULT 0.00,
  `reviewed_by` int(11) DEFAULT NULL,
  `reviewed_at` datetime DEFAULT NULL,
  `admin_remarks` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Triggers `overtime_requests`
--
DELIMITER $$
CREATE TRIGGER `trg_overtime_reviewed` AFTER UPDATE ON `overtime_requests` FOR EACH ROW BEGIN
 IF NEW.status <> OLD.status THEN
  INSERT INTO notifications(user_id,title,message_body,notification_type)
  SELECT user_id,'Overtime Request Updated',CONCAT('Your overtime request #',NEW.id,' status is ',NEW.status,'.'),'OVERTIME' FROM employees WHERE id=NEW.employee_id AND user_id IS NOT NULL;
 END IF;
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Table structure for table `payroll_periods`
--

CREATE TABLE `payroll_periods` (
  `id` int(11) NOT NULL,
  `period_name` varchar(80) NOT NULL,
  `date_from` date NOT NULL,
  `date_to` date NOT NULL,
  `cutoff_status` enum('OPEN','CLOSED','POSTED') DEFAULT 'OPEN'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `payroll_periods`
--

INSERT INTO `payroll_periods` (`id`, `period_name`, `date_from`, `date_to`, `cutoff_status`) VALUES
(1, 'May 16-31, 2026', '2026-05-16', '2026-05-31', 'OPEN');

-- --------------------------------------------------------

--
-- Table structure for table `payslip_adjustments`
--

CREATE TABLE `payslip_adjustments` (
  `id` int(11) NOT NULL,
  `payroll_period_id` int(11) NOT NULL,
  `employee_id` int(11) NOT NULL,
  `adjustment_type` enum('LATE_DEDUCTION','UNDERTIME_DEDUCTION','OVERTIME_PAY','ALLOWANCE','OTHER') NOT NULL,
  `amount` decimal(12,2) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `positions`
--

CREATE TABLE `positions` (
  `id` int(11) NOT NULL,
  `position_code` varchar(20) NOT NULL,
  `position_title` varchar(100) NOT NULL,
  `department_id` int(11) DEFAULT NULL,
  `employment_type` enum('REGULAR','CONTRACTUAL','PART_TIME','PROBATIONARY') DEFAULT 'REGULAR',
  `hourly_rate` decimal(10,2) DEFAULT 0.00,
  `status` enum('ACTIVE','INACTIVE') DEFAULT 'ACTIVE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `positions`
--

INSERT INTO `positions` (`id`, `position_code`, `position_title`, `department_id`, `employment_type`, `hourly_rate`, `status`) VALUES
(1, 'HR-ASST', 'HR Assistant', 1, 'REGULAR', 120.00, 'ACTIVE'),
(2, 'IT-TECH', 'IT Support Technician', 2, 'REGULAR', 150.00, 'ACTIVE'),
(3, 'ACC-CLK', 'Accounting Clerk', 3, 'REGULAR', 125.00, 'ACTIVE');

-- --------------------------------------------------------

--
-- Table structure for table `return_to_work_clearances`
--

CREATE TABLE `return_to_work_clearances` (
  `id` int(11) NOT NULL,
  `employee_id` int(11) NOT NULL,
  `sickness_record_id` int(11) DEFAULT NULL,
  `clearance_date` date NOT NULL,
  `physician_name` varchar(255) DEFAULT NULL,
  `medical_facility` varchar(255) DEFAULT NULL,
  `restrictions` text DEFAULT NULL,
  `fit_to_work` tinyint(1) DEFAULT 1,
  `reviewed_by` int(11) DEFAULT NULL,
  `review_date` timestamp NULL DEFAULT NULL,
  `remarks` text DEFAULT NULL,
  `status` enum('PENDING','APPROVED','REJECTED') DEFAULT 'PENDING',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `shift_assignments`
--

CREATE TABLE `shift_assignments` (
  `id` int(11) NOT NULL,
  `employee_id` int(11) NOT NULL,
  `shift_id` int(11) NOT NULL,
  `effective_from` date NOT NULL,
  `effective_to` date DEFAULT NULL,
  `day_pattern` varchar(50) DEFAULT 'MON-FRI',
  `status` enum('ACTIVE','INACTIVE') DEFAULT 'ACTIVE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `shift_assignments`
--

INSERT INTO `shift_assignments` (`id`, `employee_id`, `shift_id`, `effective_from`, `effective_to`, `day_pattern`, `status`) VALUES
(1, 1, 1, '2026-01-01', NULL, 'MON-FRI', 'ACTIVE');

-- --------------------------------------------------------

--
-- Table structure for table `shift_templates`
--

CREATE TABLE `shift_templates` (
  `id` int(11) NOT NULL,
  `shift_code` varchar(20) NOT NULL,
  `shift_name` varchar(100) NOT NULL,
  `start_time` time NOT NULL,
  `end_time` time NOT NULL,
  `break_minutes` int(11) DEFAULT 60,
  `grace_minutes` int(11) DEFAULT 10,
  `work_hours` decimal(5,2) DEFAULT 8.00,
  `status` enum('ACTIVE','INACTIVE') DEFAULT 'ACTIVE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `shift_templates`
--

INSERT INTO `shift_templates` (`id`, `shift_code`, `shift_name`, `start_time`, `end_time`, `break_minutes`, `grace_minutes`, `work_hours`, `status`) VALUES
(1, 'DAY', 'Regular Day Shift', '08:00:00', '17:00:00', 60, 10, 8.00, 'ACTIVE'),
(2, 'MID', 'Mid Shift', '10:00:00', '19:00:00', 60, 10, 8.00, 'ACTIVE');

-- --------------------------------------------------------

--
-- Table structure for table `sick_records`
--

CREATE TABLE `sick_records` (
  `id` int(11) NOT NULL,
  `employee_id` int(11) NOT NULL,
  `report_date` date NOT NULL DEFAULT curdate(),
  `diagnosis` varchar(255) NOT NULL,
  `diagnosis_code` varchar(20) DEFAULT NULL COMMENT 'ICD-10 or internal diagnosis code',
  `doctor_name` varchar(150) DEFAULT NULL,
  `recommendation` text DEFAULT NULL,
  `recovery_days` int(11) NOT NULL DEFAULT 0,
  `expected_return` date GENERATED ALWAYS AS (`report_date` + interval `recovery_days` day) STORED,
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  `admin_remarks` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `sick_records`
--

INSERT INTO `sick_records` (`id`, `employee_id`, `report_date`, `diagnosis`, `diagnosis_code`, `doctor_name`, `recommendation`, `recovery_days`, `status`, `admin_remarks`, `created_at`) VALUES
(1, 1, '2026-06-15', 'Cancer', NULL, 'Mikael Yakshun', 'Kys', 67, 'ACTIVE', 'Sure king', '2026-06-15 15:46:43');

-- --------------------------------------------------------

--
-- Table structure for table `system_settings`
--

CREATE TABLE `system_settings` (
  `id` int(11) NOT NULL,
  `setting_key` varchar(80) NOT NULL,
  `setting_value` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `system_settings`
--

INSERT INTO `system_settings` (`id`, `setting_key`, `setting_value`, `description`) VALUES
(1, 'company_name', 'GR 8 Demo Corporation', 'Displayed company name'),
(2, 'default_grace_minutes', '10', 'Allowed late grace period');

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id` int(11) NOT NULL,
  `username` varchar(50) NOT NULL,
  `password_hash` char(64) NOT NULL,
  `role` enum('ADMIN','EMPLOYEE') NOT NULL DEFAULT 'EMPLOYEE',
  `full_name` varchar(120) NOT NULL,
  `email` varchar(120) NOT NULL,
  `phone` varchar(30) DEFAULT NULL,
  `status` enum('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`id`, `username`, `password_hash`, `role`, `full_name`, `email`, `phone`, `status`, `created_at`) VALUES
(1, 'admin', 'e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7', 'ADMIN', 'System Administrator', 'admin@company.local', '09170000001', 'ACTIVE', '2026-06-01 13:57:45'),
(2, 'employee1', '3e7c19576488862816f13b512cacf3e4ba97dd97243ea0bd6a2ad1642d86ba72', 'EMPLOYEE', 'Justin Nicolai Robert Bautista y Elijandro Banaag Algernon von Mendoza II the Great van Baskerville', 'employee1@company.local', '09170000002', 'ACTIVE', '2026-06-01 13:57:45');

-- --------------------------------------------------------

--
-- Stand-in structure for view `v_daily_attendance`
-- (See below for the actual view)
--
CREATE TABLE `v_daily_attendance` (
`id` int(11)
,`attendance_date` date
,`employee_no` varchar(30)
,`full_name` varchar(120)
,`department_name` varchar(100)
,`shift_name` varchar(100)
,`clock_in` datetime
,`clock_out` datetime
,`late_minutes` int(11)
,`undertime_minutes` int(11)
,`overtime_minutes` int(11)
,`attendance_status` enum('PRESENT','LATE','ABSENT','ON_LEAVE','REST_DAY','HOLIDAY','CORRECTED')
);

-- --------------------------------------------------------

--
-- Stand-in structure for view `v_employee_medical`
-- (See below for the actual view)
--
CREATE TABLE `v_employee_medical` (
`employee_no` varchar(30)
,`full_name` varchar(120)
,`department_name` varchar(100)
,`blood_type` enum('A+','A-','AB+','AB-','B+','B-','O+','O-')
,`allergy` varchar(255)
,`existing_condition` varchar(255)
,`emergency_notes` text
,`medical_certificate` varchar(500)
,`workplace_injury_report` varchar(500)
,`health_declaration` varchar(500)
,`fitness_to_work_clearance` varchar(500)
,`wellness_activity` varchar(500)
);

-- --------------------------------------------------------

--
-- Stand-in structure for view `v_employee_roster`
-- (See below for the actual view)
--
CREATE TABLE `v_employee_roster` (
`id` int(11)
,`employee_no` varchar(30)
,`full_name` varchar(120)
,`email` varchar(120)
,`department_name` varchar(100)
,`position_title` varchar(100)
,`date_hired` date
,`employment_status` enum('ACTIVE','ON_LEAVE','RESIGNED','TERMINATED')
);

-- --------------------------------------------------------

--
-- Stand-in structure for view `v_late_absence_summary`
-- (See below for the actual view)
--
CREATE TABLE `v_late_absence_summary` (
`employee_no` varchar(30)
,`full_name` varchar(120)
,`department_name` varchar(100)
,`absent_days` bigint(21)
,`late_minutes` decimal(32,0)
,`undertime_minutes` decimal(32,0)
);

-- --------------------------------------------------------

--
-- Stand-in structure for view `v_leave_monitoring`
-- (See below for the actual view)
--
CREATE TABLE `v_leave_monitoring` (
`id` int(11)
,`employee_no` varchar(30)
,`full_name` varchar(120)
,`leave_name` varchar(100)
,`start_date` date
,`end_date` date
,`total_days` decimal(5,2)
,`reason` varchar(255)
,`status` enum('PENDING','APPROVED','REJECTED','CANCELLED')
,`filed_at` timestamp
);

-- --------------------------------------------------------

--
-- Stand-in structure for view `v_overtime_monitoring`
-- (See below for the actual view)
--
CREATE TABLE `v_overtime_monitoring` (
`id` int(11)
,`employee_no` varchar(30)
,`full_name` varchar(120)
,`overtime_date` date
,`requested_hours` decimal(5,2)
,`approved_hours` decimal(5,2)
,`purpose` varchar(255)
,`status` enum('PENDING','APPROVED','REJECTED','CANCELLED')
);

-- --------------------------------------------------------

--
-- Stand-in structure for view `v_payroll_attendance_summary`
-- (See below for the actual view)
--
CREATE TABLE `v_payroll_attendance_summary` (
`period_name` varchar(80)
,`employee_no` varchar(30)
,`full_name` varchar(120)
,`present_days` decimal(5,2)
,`absent_days` decimal(5,2)
,`leave_days` decimal(5,2)
,`late_minutes` int(11)
,`undertime_minutes` int(11)
,`overtime_hours` decimal(7,2)
);

-- --------------------------------------------------------

--
-- Stand-in structure for view `v_sick_monitoring`
-- (See below for the actual view)
--
CREATE TABLE `v_sick_monitoring` (
`id` int(11)
,`employee_no` varchar(30)
,`full_name` varchar(120)
,`department_name` varchar(100)
,`report_date` date
,`diagnosis_code` varchar(20)
,`diagnosis` varchar(255)
,`doctor_name` varchar(150)
,`recommendation` text
,`recovery_days` int(11)
,`expected_return` date
,`days_remaining` int(7)
,`status` varchar(20)
,`admin_remarks` varchar(255)
);

-- --------------------------------------------------------

--
-- Table structure for table `work_schedules`
--

CREATE TABLE `work_schedules` (
  `id` int(11) NOT NULL,
  `employee_id` int(11) NOT NULL,
  `schedule_date` date NOT NULL,
  `shift_id` int(11) NOT NULL,
  `schedule_status` enum('WORKDAY','REST_DAY','HOLIDAY','ON_LEAVE') DEFAULT 'WORKDAY',
  `remarks` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `work_schedules`
--

INSERT INTO `work_schedules` (`id`, `employee_id`, `schedule_date`, `shift_id`, `schedule_status`, `remarks`) VALUES
(1, 1, '2026-06-01', 1, 'WORKDAY', 'Regular schedule');

-- --------------------------------------------------------

--
-- Structure for view `v_daily_attendance`
--
DROP TABLE IF EXISTS `v_daily_attendance`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `v_daily_attendance`  AS SELECT `ar`.`id` AS `id`, `ar`.`attendance_date` AS `attendance_date`, `e`.`employee_no` AS `employee_no`, `u`.`full_name` AS `full_name`, `d`.`department_name` AS `department_name`, `s`.`shift_name` AS `shift_name`, `ar`.`clock_in` AS `clock_in`, `ar`.`clock_out` AS `clock_out`, `ar`.`late_minutes` AS `late_minutes`, `ar`.`undertime_minutes` AS `undertime_minutes`, `ar`.`overtime_minutes` AS `overtime_minutes`, `ar`.`attendance_status` AS `attendance_status` FROM ((((`attendance_records` `ar` join `employees` `e` on(`ar`.`employee_id` = `e`.`id`)) join `users` `u` on(`e`.`user_id` = `u`.`id`)) left join `departments` `d` on(`e`.`department_id` = `d`.`id`)) left join `shift_templates` `s` on(`ar`.`shift_id` = `s`.`id`)) ;

-- --------------------------------------------------------

--
-- Structure for view `v_employee_medical`
--
DROP TABLE IF EXISTS `v_employee_medical`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `v_employee_medical`  AS SELECT `e`.`employee_no` AS `employee_no`, `u`.`full_name` AS `full_name`, `d`.`department_name` AS `department_name`, `m`.`blood_type` AS `blood_type`, `m`.`allergy` AS `allergy`, `m`.`existing_condition` AS `existing_condition`, `m`.`emergency_notes` AS `emergency_notes`, `m`.`medical_certificate` AS `medical_certificate`, `m`.`workplace_injury_report` AS `workplace_injury_report`, `m`.`health_declaration` AS `health_declaration`, `m`.`fitness_to_work_clearance` AS `fitness_to_work_clearance`, `m`.`wellness_activity` AS `wellness_activity` FROM (((`employee_medical_details` `m` join `employees` `e` on(`m`.`employee_id` = `e`.`id`)) join `users` `u` on(`e`.`user_id` = `u`.`id`)) left join `departments` `d` on(`e`.`department_id` = `d`.`id`)) ;

-- --------------------------------------------------------

--
-- Structure for view `v_employee_roster`
--
DROP TABLE IF EXISTS `v_employee_roster`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `v_employee_roster`  AS SELECT `e`.`id` AS `id`, `e`.`employee_no` AS `employee_no`, `u`.`full_name` AS `full_name`, `u`.`email` AS `email`, `d`.`department_name` AS `department_name`, `p`.`position_title` AS `position_title`, `e`.`date_hired` AS `date_hired`, `e`.`employment_status` AS `employment_status` FROM (((`employees` `e` join `users` `u` on(`e`.`user_id` = `u`.`id`)) left join `departments` `d` on(`e`.`department_id` = `d`.`id`)) left join `positions` `p` on(`e`.`position_id` = `p`.`id`)) ;

-- --------------------------------------------------------

--
-- Structure for view `v_late_absence_summary`
--
DROP TABLE IF EXISTS `v_late_absence_summary`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `v_late_absence_summary`  AS SELECT `e`.`employee_no` AS `employee_no`, `u`.`full_name` AS `full_name`, `d`.`department_name` AS `department_name`, count(case when `ar`.`attendance_status` = 'ABSENT' then 1 end) AS `absent_days`, sum(`ar`.`late_minutes`) AS `late_minutes`, sum(`ar`.`undertime_minutes`) AS `undertime_minutes` FROM (((`employees` `e` join `users` `u` on(`e`.`user_id` = `u`.`id`)) left join `departments` `d` on(`e`.`department_id` = `d`.`id`)) left join `attendance_records` `ar` on(`e`.`id` = `ar`.`employee_id`)) GROUP BY `e`.`id`, `e`.`employee_no`, `u`.`full_name`, `d`.`department_name` ;

-- --------------------------------------------------------

--
-- Structure for view `v_leave_monitoring`
--
DROP TABLE IF EXISTS `v_leave_monitoring`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `v_leave_monitoring`  AS SELECT `lr`.`id` AS `id`, `e`.`employee_no` AS `employee_no`, `u`.`full_name` AS `full_name`, `lt`.`leave_name` AS `leave_name`, `lr`.`start_date` AS `start_date`, `lr`.`end_date` AS `end_date`, `lr`.`total_days` AS `total_days`, `lr`.`reason` AS `reason`, `lr`.`status` AS `status`, `lr`.`filed_at` AS `filed_at` FROM (((`leave_requests` `lr` join `employees` `e` on(`lr`.`employee_id` = `e`.`id`)) join `users` `u` on(`e`.`user_id` = `u`.`id`)) join `leave_types` `lt` on(`lr`.`leave_type_id` = `lt`.`id`)) ;

-- --------------------------------------------------------

--
-- Structure for view `v_overtime_monitoring`
--
DROP TABLE IF EXISTS `v_overtime_monitoring`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `v_overtime_monitoring`  AS SELECT `o`.`id` AS `id`, `e`.`employee_no` AS `employee_no`, `u`.`full_name` AS `full_name`, `o`.`overtime_date` AS `overtime_date`, `o`.`requested_hours` AS `requested_hours`, `o`.`approved_hours` AS `approved_hours`, `o`.`purpose` AS `purpose`, `o`.`status` AS `status` FROM ((`overtime_requests` `o` join `employees` `e` on(`o`.`employee_id` = `e`.`id`)) join `users` `u` on(`e`.`user_id` = `u`.`id`)) ;

-- --------------------------------------------------------

--
-- Structure for view `v_payroll_attendance_summary`
--
DROP TABLE IF EXISTS `v_payroll_attendance_summary`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `v_payroll_attendance_summary`  AS SELECT `pp`.`period_name` AS `period_name`, `e`.`employee_no` AS `employee_no`, `u`.`full_name` AS `full_name`, `s`.`present_days` AS `present_days`, `s`.`absent_days` AS `absent_days`, `s`.`leave_days` AS `leave_days`, `s`.`late_minutes` AS `late_minutes`, `s`.`undertime_minutes` AS `undertime_minutes`, `s`.`overtime_hours` AS `overtime_hours` FROM (((`attendance_summaries` `s` join `payroll_periods` `pp` on(`s`.`payroll_period_id` = `pp`.`id`)) join `employees` `e` on(`s`.`employee_id` = `e`.`id`)) join `users` `u` on(`e`.`user_id` = `u`.`id`)) ;

-- --------------------------------------------------------

--
-- Structure for view `v_sick_monitoring`
--
DROP TABLE IF EXISTS `v_sick_monitoring`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `v_sick_monitoring`  AS SELECT `sr`.`id` AS `id`, `e`.`employee_no` AS `employee_no`, `u`.`full_name` AS `full_name`, `d`.`department_name` AS `department_name`, `sr`.`report_date` AS `report_date`, `sr`.`diagnosis_code` AS `diagnosis_code`, `sr`.`diagnosis` AS `diagnosis`, `sr`.`doctor_name` AS `doctor_name`, `sr`.`recommendation` AS `recommendation`, `sr`.`recovery_days` AS `recovery_days`, `sr`.`expected_return` AS `expected_return`, greatest(0,to_days(`sr`.`expected_return`) - to_days(curdate())) AS `days_remaining`, `sr`.`status` AS `status`, `sr`.`admin_remarks` AS `admin_remarks` FROM (((`sick_records` `sr` join `employees` `e` on(`sr`.`employee_id` = `e`.`id`)) join `users` `u` on(`e`.`user_id` = `u`.`id`)) left join `departments` `d` on(`e`.`department_id` = `d`.`id`)) ORDER BY `sr`.`report_date` DESC ;

--
-- Indexes for dumped tables
--

--
-- Indexes for table `announcements`
--
ALTER TABLE `announcements`
  ADD PRIMARY KEY (`id`),
  ADD KEY `posted_by` (`posted_by`);

--
-- Indexes for table `attendance_corrections`
--
ALTER TABLE `attendance_corrections`
  ADD PRIMARY KEY (`id`),
  ADD KEY `employee_id` (`employee_id`),
  ADD KEY `attendance_id` (`attendance_id`),
  ADD KEY `reviewed_by` (`reviewed_by`),
  ADD KEY `idx_corrections_status` (`status`);

--
-- Indexes for table `attendance_locations`
--
ALTER TABLE `attendance_locations`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `location_code` (`location_code`);

--
-- Indexes for table `attendance_logs`
--
ALTER TABLE `attendance_logs`
  ADD PRIMARY KEY (`id`),
  ADD KEY `attendance_id` (`attendance_id`),
  ADD KEY `employee_id` (`employee_id`),
  ADD KEY `device_id` (`device_id`);

--
-- Indexes for table `attendance_records`
--
ALTER TABLE `attendance_records`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_daily_attendance` (`employee_id`,`attendance_date`),
  ADD KEY `shift_id` (`shift_id`),
  ADD KEY `location_id` (`location_id`),
  ADD KEY `idx_attendance_date` (`attendance_date`);

--
-- Indexes for table `attendance_summaries`
--
ALTER TABLE `attendance_summaries`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_summary` (`payroll_period_id`,`employee_id`),
  ADD KEY `employee_id` (`employee_id`);

--
-- Indexes for table `audit_logs`
--
ALTER TABLE `audit_logs`
  ADD PRIMARY KEY (`id`),
  ADD KEY `user_id` (`user_id`);

--
-- Indexes for table `biometric_devices`
--
ALTER TABLE `biometric_devices`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `device_code` (`device_code`),
  ADD KEY `location_id` (`location_id`);

--
-- Indexes for table `departments`
--
ALTER TABLE `departments`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `department_code` (`department_code`);

--
-- Indexes for table `employees`
--
ALTER TABLE `employees`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `employee_no` (`employee_no`),
  ADD UNIQUE KEY `user_id` (`user_id`),
  ADD KEY `department_id` (`department_id`),
  ADD KEY `position_id` (`position_id`),
  ADD KEY `idx_employee_name` (`employee_no`);

--
-- Indexes for table `employee_medical_details`
--
ALTER TABLE `employee_medical_details`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `employee_id` (`employee_id`),
  ADD KEY `idx_medical_employee` (`employee_id`);

--
-- Indexes for table `holidays`
--
ALTER TABLE `holidays`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `holiday_date` (`holiday_date`);

--
-- Indexes for table `leave_requests`
--
ALTER TABLE `leave_requests`
  ADD PRIMARY KEY (`id`),
  ADD KEY `employee_id` (`employee_id`),
  ADD KEY `leave_type_id` (`leave_type_id`),
  ADD KEY `reviewed_by` (`reviewed_by`),
  ADD KEY `idx_leave_status` (`status`);

--
-- Indexes for table `leave_types`
--
ALTER TABLE `leave_types`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `leave_code` (`leave_code`);

--
-- Indexes for table `messages`
--
ALTER TABLE `messages`
  ADD PRIMARY KEY (`id`),
  ADD KEY `sender_id` (`sender_id`),
  ADD KEY `receiver_id` (`receiver_id`);

--
-- Indexes for table `notifications`
--
ALTER TABLE `notifications`
  ADD PRIMARY KEY (`id`),
  ADD KEY `user_id` (`user_id`);

--
-- Indexes for table `overtime_requests`
--
ALTER TABLE `overtime_requests`
  ADD PRIMARY KEY (`id`),
  ADD KEY `employee_id` (`employee_id`),
  ADD KEY `reviewed_by` (`reviewed_by`),
  ADD KEY `idx_overtime_status` (`status`);

--
-- Indexes for table `payroll_periods`
--
ALTER TABLE `payroll_periods`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_payroll_period` (`date_from`,`date_to`);

--
-- Indexes for table `payslip_adjustments`
--
ALTER TABLE `payslip_adjustments`
  ADD PRIMARY KEY (`id`),
  ADD KEY `payroll_period_id` (`payroll_period_id`),
  ADD KEY `employee_id` (`employee_id`);

--
-- Indexes for table `positions`
--
ALTER TABLE `positions`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `position_code` (`position_code`),
  ADD KEY `department_id` (`department_id`);

--
-- Indexes for table `return_to_work_clearances`
--
ALTER TABLE `return_to_work_clearances`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `shift_assignments`
--
ALTER TABLE `shift_assignments`
  ADD PRIMARY KEY (`id`),
  ADD KEY `employee_id` (`employee_id`),
  ADD KEY `shift_id` (`shift_id`);

--
-- Indexes for table `shift_templates`
--
ALTER TABLE `shift_templates`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `shift_code` (`shift_code`);

--
-- Indexes for table `sick_records`
--
ALTER TABLE `sick_records`
  ADD PRIMARY KEY (`id`),
  ADD KEY `employee_id` (`employee_id`);

--
-- Indexes for table `system_settings`
--
ALTER TABLE `system_settings`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `setting_key` (`setting_key`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `username` (`username`),
  ADD UNIQUE KEY `email` (`email`);

--
-- Indexes for table `work_schedules`
--
ALTER TABLE `work_schedules`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_employee_schedule` (`employee_id`,`schedule_date`),
  ADD KEY `shift_id` (`shift_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `announcements`
--
ALTER TABLE `announcements`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `attendance_corrections`
--
ALTER TABLE `attendance_corrections`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `attendance_locations`
--
ALTER TABLE `attendance_locations`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `attendance_logs`
--
ALTER TABLE `attendance_logs`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT for table `attendance_records`
--
ALTER TABLE `attendance_records`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT for table `attendance_summaries`
--
ALTER TABLE `attendance_summaries`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `audit_logs`
--
ALTER TABLE `audit_logs`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=57;

--
-- AUTO_INCREMENT for table `biometric_devices`
--
ALTER TABLE `biometric_devices`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `departments`
--
ALTER TABLE `departments`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `employees`
--
ALTER TABLE `employees`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `employee_medical_details`
--
ALTER TABLE `employee_medical_details`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `holidays`
--
ALTER TABLE `holidays`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `leave_requests`
--
ALTER TABLE `leave_requests`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `leave_types`
--
ALTER TABLE `leave_types`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT for table `messages`
--
ALTER TABLE `messages`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `notifications`
--
ALTER TABLE `notifications`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `overtime_requests`
--
ALTER TABLE `overtime_requests`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `payroll_periods`
--
ALTER TABLE `payroll_periods`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `payslip_adjustments`
--
ALTER TABLE `payslip_adjustments`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `positions`
--
ALTER TABLE `positions`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `return_to_work_clearances`
--
ALTER TABLE `return_to_work_clearances`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `shift_assignments`
--
ALTER TABLE `shift_assignments`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `shift_templates`
--
ALTER TABLE `shift_templates`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `sick_records`
--
ALTER TABLE `sick_records`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `system_settings`
--
ALTER TABLE `system_settings`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `work_schedules`
--
ALTER TABLE `work_schedules`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `announcements`
--
ALTER TABLE `announcements`
  ADD CONSTRAINT `announcements_ibfk_1` FOREIGN KEY (`posted_by`) REFERENCES `users` (`id`) ON DELETE SET NULL;

--
-- Constraints for table `attendance_corrections`
--
ALTER TABLE `attendance_corrections`
  ADD CONSTRAINT `attendance_corrections_ibfk_1` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `attendance_corrections_ibfk_2` FOREIGN KEY (`attendance_id`) REFERENCES `attendance_records` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `attendance_corrections_ibfk_3` FOREIGN KEY (`reviewed_by`) REFERENCES `users` (`id`) ON DELETE SET NULL;

--
-- Constraints for table `attendance_logs`
--
ALTER TABLE `attendance_logs`
  ADD CONSTRAINT `attendance_logs_ibfk_1` FOREIGN KEY (`attendance_id`) REFERENCES `attendance_records` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `attendance_logs_ibfk_2` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `attendance_logs_ibfk_3` FOREIGN KEY (`device_id`) REFERENCES `biometric_devices` (`id`) ON DELETE SET NULL;

--
-- Constraints for table `attendance_records`
--
ALTER TABLE `attendance_records`
  ADD CONSTRAINT `attendance_records_ibfk_1` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `attendance_records_ibfk_2` FOREIGN KEY (`shift_id`) REFERENCES `shift_templates` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `attendance_records_ibfk_3` FOREIGN KEY (`location_id`) REFERENCES `attendance_locations` (`id`) ON DELETE SET NULL;

--
-- Constraints for table `attendance_summaries`
--
ALTER TABLE `attendance_summaries`
  ADD CONSTRAINT `attendance_summaries_ibfk_1` FOREIGN KEY (`payroll_period_id`) REFERENCES `payroll_periods` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `attendance_summaries_ibfk_2` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `audit_logs`
--
ALTER TABLE `audit_logs`
  ADD CONSTRAINT `audit_logs_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL;

--
-- Constraints for table `biometric_devices`
--
ALTER TABLE `biometric_devices`
  ADD CONSTRAINT `biometric_devices_ibfk_1` FOREIGN KEY (`location_id`) REFERENCES `attendance_locations` (`id`) ON DELETE SET NULL;

--
-- Constraints for table `employees`
--
ALTER TABLE `employees`
  ADD CONSTRAINT `employees_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `employees_ibfk_2` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `employees_ibfk_3` FOREIGN KEY (`position_id`) REFERENCES `positions` (`id`) ON DELETE SET NULL;

--
-- Constraints for table `employee_medical_details`
--
ALTER TABLE `employee_medical_details`
  ADD CONSTRAINT `employee_medical_details_ibfk_1` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `leave_requests`
--
ALTER TABLE `leave_requests`
  ADD CONSTRAINT `leave_requests_ibfk_1` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `leave_requests_ibfk_2` FOREIGN KEY (`leave_type_id`) REFERENCES `leave_types` (`id`),
  ADD CONSTRAINT `leave_requests_ibfk_3` FOREIGN KEY (`reviewed_by`) REFERENCES `users` (`id`) ON DELETE SET NULL;

--
-- Constraints for table `messages`
--
ALTER TABLE `messages`
  ADD CONSTRAINT `messages_ibfk_1` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `messages_ibfk_2` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `notifications`
--
ALTER TABLE `notifications`
  ADD CONSTRAINT `notifications_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `overtime_requests`
--
ALTER TABLE `overtime_requests`
  ADD CONSTRAINT `overtime_requests_ibfk_1` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `overtime_requests_ibfk_2` FOREIGN KEY (`reviewed_by`) REFERENCES `users` (`id`) ON DELETE SET NULL;

--
-- Constraints for table `payslip_adjustments`
--
ALTER TABLE `payslip_adjustments`
  ADD CONSTRAINT `payslip_adjustments_ibfk_1` FOREIGN KEY (`payroll_period_id`) REFERENCES `payroll_periods` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `payslip_adjustments_ibfk_2` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `positions`
--
ALTER TABLE `positions`
  ADD CONSTRAINT `positions_ibfk_1` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`) ON DELETE SET NULL;

--
-- Constraints for table `shift_assignments`
--
ALTER TABLE `shift_assignments`
  ADD CONSTRAINT `shift_assignments_ibfk_1` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `shift_assignments_ibfk_2` FOREIGN KEY (`shift_id`) REFERENCES `shift_templates` (`id`);

--
-- Constraints for table `sick_records`
--
ALTER TABLE `sick_records`
  ADD CONSTRAINT `sick_records_ibfk_1` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`);

--
-- Constraints for table `work_schedules`
--
ALTER TABLE `work_schedules`
  ADD CONSTRAINT `work_schedules_ibfk_1` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `work_schedules_ibfk_2` FOREIGN KEY (`shift_id`) REFERENCES `shift_templates` (`id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
