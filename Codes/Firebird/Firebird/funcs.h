/**
list of all functions
**/




void buzzer_on (void); 
void buzzer_off (void);
void lcd_port_config (void);
void adc_pin_config (void);
void motion_pin_config (void) ;
void port_init();
void timer5_init();
void adc_init();
void motion_set (unsigned char Direction);
void forward (void) ;
void stop (void);
void init_devices (void);
void print_sensor(char row, char coloumn,unsigned char channel); 
void print_sensor_data();


void read_sensors(); //read sensor values and write them into left,right vars
void turn_right();  //turns right until it sees a white line
void turn_left(); //same as above but left
void go_upto_next_cross();  //moves upto next cross
void dfs_init();   //initialsation for dfs to run
void set_dest(int i); //set the destination the value of dest[i]
void init();  //initialization
int dfs(int a ,int b); //the dfs function
void velocity (unsigned char left_motor, unsigned char right_motor);  //set the velocity of right and left motors
void mod_velocity(int l,int r); //a fucntion which scales the velcity 

