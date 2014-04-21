SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL,ALLOW_INVALID_DATES';

DROP SCHEMA IF EXISTS `heracles` ;
CREATE SCHEMA IF NOT EXISTS `heracles` DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ;
USE `heracles` ;

-- -----------------------------------------------------
-- Table `heracles`.`packages`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `heracles`.`packages` ;

CREATE  TABLE IF NOT EXISTS `heracles`.`packages` (
  `checksum` VARCHAR(32) NOT NULL ,
  `name` VARCHAR(255) NOT NULL ,
  `label` VARCHAR(255) NULL ,
  `versionCode` INT NULL ,
  `versionLabel` VARCHAR(45) NULL ,
  `minSdk` INT NULL ,
  `targetSdk` INT NULL ,
  `size` INT NULL ,
  PRIMARY KEY (`checksum`) )
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `heracles`.`permissions`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `heracles`.`permissions` ;

CREATE  TABLE IF NOT EXISTS `heracles`.`permissions` (
  `name` VARCHAR(200) NOT NULL ,
  `protectionLevel` INT NULL ,
  `group` VARCHAR(45) NULL ,
  `label` VARCHAR(45) NULL ,
  PRIMARY KEY (`name`) )
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `heracles`.`users`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `heracles`.`users` ;

CREATE  TABLE IF NOT EXISTS `heracles`.`users` (
  `name` VARCHAR(45) NOT NULL ,
  PRIMARY KEY (`name`) )
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `heracles`.`devices`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `heracles`.`devices` ;

CREATE  TABLE IF NOT EXISTS `heracles`.`devices` (
  `uuid` VARCHAR(16) NOT NULL ,
  `make` VARCHAR(45) NULL ,
  `model` VARCHAR(45) NULL ,
  `carrier` VARCHAR(45) NULL ,
  `rom` VARCHAR(90) NULL ,
  `version` VARCHAR(10) NULL ,
  `user` VARCHAR(45) NULL ,
  PRIMARY KEY (`uuid`) ,
  INDEX `fk_devices_users1_idx` (`user` ASC) ,
  CONSTRAINT `fk_devices_users1`
    FOREIGN KEY (`user` )
    REFERENCES `heracles`.`users` (`name` )
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `heracles`.`user_profiles`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `heracles`.`user_profiles` ;

CREATE  TABLE IF NOT EXISTS `heracles`.`user_profiles` (
  `permission` VARCHAR(200) NOT NULL ,
  `active` TINYINT(1) NOT NULL ,
  `package` VARCHAR(32) NOT NULL ,
  `user` VARCHAR(45) NOT NULL ,
  PRIMARY KEY (`permission`, `package`, `user`) ,
  INDEX `fk_packages_has_permissions_permissions1_idx` (`permission` ASC) ,
  INDEX `fk_user_profile_packages1_idx` (`package` ASC) ,
  INDEX `fk_user_profile_users1_idx` (`user` ASC) ,
  CONSTRAINT `fk_packages_has_permissions_permissions1`
    FOREIGN KEY (`permission` )
    REFERENCES `heracles`.`permissions` (`name` )
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_user_profile_packages1`
    FOREIGN KEY (`package` )
    REFERENCES `heracles`.`packages` (`checksum` )
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_user_profile_users1`
    FOREIGN KEY (`user` )
    REFERENCES `heracles`.`users` (`name` )
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `heracles`.`device_profiles`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `heracles`.`device_profiles` ;

CREATE  TABLE IF NOT EXISTS `heracles`.`device_profiles` (
  `permission` VARCHAR(200) NOT NULL ,
  `uuid` VARCHAR(16) NOT NULL ,
  `active` TINYINT(1) NOT NULL ,
  `package` VARCHAR(32) NOT NULL ,
  PRIMARY KEY (`permission`, `uuid`, `package`) ,
  INDEX `fk_packages_has_permissions_permissions2_idx` (`permission` ASC) ,
  INDEX `fk_packages_has_permissions_devices1_idx` (`uuid` ASC) ,
  INDEX `fk_device_profile_packages1_idx` (`package` ASC) ,
  CONSTRAINT `fk_device_profile_permissions`
    FOREIGN KEY (`permission` )
    REFERENCES `heracles`.`permissions` (`name` )
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_device_profile_device`
    FOREIGN KEY (`uuid` )
    REFERENCES `heracles`.`devices` (`uuid` )
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_device_profile_packages1`
    FOREIGN KEY (`package` )
    REFERENCES `heracles`.`packages` (`checksum` )
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `heracles`.`requests`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `heracles`.`requests` ;

CREATE  TABLE IF NOT EXISTS `heracles`.`requests` (
  `permission` VARCHAR(200) NOT NULL ,
  `package` VARCHAR(32) NOT NULL ,
  PRIMARY KEY (`permission`, `package`) ,
  INDEX `fk_packages_has_permissions_permissions3_idx` (`permission` ASC) ,
  INDEX `fk_requests_packages1_idx` (`package` ASC) ,
  CONSTRAINT `fk_packages_has_permissions_permissions3`
    FOREIGN KEY (`permission` )
    REFERENCES `heracles`.`permissions` (`name` )
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_requests_packages1`
    FOREIGN KEY (`package` )
    REFERENCES `heracles`.`packages` (`checksum` )
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `heracles`.`markets`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `heracles`.`markets` ;

CREATE  TABLE IF NOT EXISTS `heracles`.`markets` (
  `market` VARCHAR(45) NOT NULL ,
  `package` VARCHAR(32) NOT NULL ,
  `category` VARCHAR(45) NULL ,
  `rating` DECIMAL(4,2) NULL ,
  `votes` INT NULL ,
  `downloads` INT NULL ,
  `price` DECIMAL(7,2) NULL ,
  `author` VARCHAR(45) NULL ,
  `datePublished` DATETIME NULL ,
  `url` VARCHAR(200) NULL ,
  PRIMARY KEY (`market`, `package`) ,
  CONSTRAINT `fk_markets_packages1`
    FOREIGN KEY (`package` )
    REFERENCES `heracles`.`packages` (`checksum` )
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB;

USE `heracles` ;


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
