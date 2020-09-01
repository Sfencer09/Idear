-- create database Idear;

use Idear;

Drop Table if exists Images;
Drop Table if exists Users;
Drop procedure if exists addUser;
drop procedure if exists addImage;
drop procedure if exists getSalt;
drop procedure if exists verifyHash;
drop procedure if exists testHash;

CREATE Table Users (
UserID int primary key NOT NULL auto_increment,
email varchar(254),
passwordSalt varchar(16),
passwordHash varbinary(32),
lastLogin datetime /*DEFAULT ON UPDATE*/
);

create Table Images (
ImageID int NOT NULL auto_increment,
primary key(ImageID),
UserID int NOT NULL,
foreign key (UserID) references Users(UserID),
baseImage longblob,
modifiedImage longblob,
ancillaryData longtext,
rawBlocks longtext,
returnedText longtext
);

DELIMITER //
CREATE PROCEDURE addUser(IN Eml varchar(254), IN Slt varchar(16), IN Hsh varbinary(32))
    BEGIN
        insert into
        users (Email, PasswordSalt, PasswordHash, LastLogin)
        VALUES (Eml, Slt, Hsh, CURRENT_DATE);
    END//

CREATE PROCEDURE addImage(IN UsID int, IN bsImg longblob, IN fnImg longblob, IN ancData text, IN Tsblks text, IN rtnText text)
    BEGIN
        insert into
        images (UserID, BaseImage, FinalImage, AncillaryData, Tessblocks, ReturnedText)
        VALUES (UsID, bsImg, fnImg, ancData, Tsblks, rtnText);
    END//
    
CREATE PROCEDURE getSalt(IN Eml varchar(254))
    BEGIN
        declare Salt varchar(16);
        if exists(select PasswordSalt from users where (Email = Eml)) THEN
            SET Salt = (select PasswordSalt from users where (Email = Eml));
        ELSE
            SET Salt = '';
        end if;
        select Salt;
    END//

CREATE PROCEDURE verifyHash(IN Eml varchar(254), IN Hsh varbinary(32))
    BEGIN
		declare id int;
        if exists(select UserID from users where (users.email = Eml) and (hsh = users.passwordHash)) THEN
            SET id = (select UserID from users where (users.email = Eml) and (hsh = users.passwordHash));
            update users set lastLogin=current_date where users.userID = id;
        ELSE
            SET id = -1;
        end if;
        select id;
    END//
DELIMITER ;

call addUser("test@gmail.com", "0000000000000000", x'1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF');

call getSalt("test@gmail.com");
call verifyHash("test@gmail.com", x'1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF');
call verifyHash("test@gmail.com", x'1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEE');