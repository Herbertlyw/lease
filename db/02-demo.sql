SET NAMES utf8mb4;
INSERT INTO province_info (id,name) VALUES (1,'浙江省');
INSERT INTO city_info (id,name,province_id) VALUES (1,'杭州市',1);
INSERT INTO district_info (id,name,city_id) VALUES (1,'西湖区',1);
INSERT INTO apartment_info (id,name,province_id,province_name,city_id,city_name,district_id,district_name,address_detail,is_release)
VALUES (1,'青禾公寓',1,'浙江省',1,'杭州市',1,'西湖区','示例路 10 号',1);
INSERT INTO room_info (id,room_number,rent,apartment_id,is_release)
VALUES (1,'101',1800,1,1),(2,'102',2400,1,1),(3,'201',1500,1,0);
INSERT INTO payment_type (id,name,pay_month_count) VALUES (1,'月付','1');
INSERT INTO room_payment_type (room_id,payment_type_id) VALUES (1,1),(2,1);
INSERT INTO lease_term (id,month_count,unit) VALUES (1,12,'月');
INSERT INTO room_lease_term (room_id,lease_term_id) VALUES (1,1);
INSERT INTO label_info (id,type,name) VALUES (1,2,'朝南');
INSERT INTO room_label (room_id,label_id) VALUES (1,1);
INSERT INTO system_user (username,password,name,type,status) VALUES ('demo','$2y$10$s0isJPZTwheSSeu/5WWM6OvVL2SgyL2cgZT8DFtS4iOl3t7xOAUaG','演示管理员',1,1);
