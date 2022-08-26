package com.example.curse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

public class Memory extends Element{

    private final String cmd_1 = "top -bn 1 -i -c";
    private String regex = "KiB Mem :";
    private double mb_used, mb_total; //переменные для хранения значений используемой озу и общее значение озу

    public Memory(){
        this.setCmd(cmd_1);
        this.setRegex(regex);
        try {
            this.parse();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.setName("RAM usage");
        this.setMeasure("MB");
    }

    public double getMb_used(){ //геттер для используемой памяти
        return this.mb_used;
    }

    public double getMb_total(){ //геттер для полной памяти
        return this.mb_total;
    }

    @Override
    public void show(){ //вывод в консоль информации
        System.out.print(this.getName() + ": " + this.getMb_used() + " " + this.getMeasure() + " | Total: " + this.getMb_total() + " " + this.getMeasure() + " [" + this.getDate() + "]\n\n" );
    }

    @Override
    public void grab(){//вывод команды - одна строка, поэтому ищем циклом сразу в массиве частей
        ArrayList<String> parse_res; //массив строк для копирования результатов команды
        parse_res = this.getResult(); //копирование вывода команды
        double temp1, temp2; //вспомогательные переменные, нужны для округления полученных значений
        String temp; //строка для разбиения
        String parts[] = parse_res.get(0).split(" "); //массив частей строки, разбитых по пробелу
        for(int i = 0; i < parts.length; i++){ //пока не конец вывода
            if(parts[i].contains("used")){ //если строка содержит used, значит она нужная нам
                temp1 = (Double.valueOf(parts[i-1]) / 1024); //получаем значение перед этим словом, приводим его к типу double, сокрощаем до МБ
                temp = String.format("%.2f",temp1); //округляем полученное значение
                this.mb_used = Double.valueOf(temp); //устанавливаем его
            }
            if(parts[i].contains("total")){  //тот же алгоритм, как и с used
                temp2 = (Double.valueOf(parts[i-1]) / 1024);
                temp = String.format("%.2f",temp2);
                this.mb_total = Double.valueOf(temp);
            }
        }
        this.recordInDB();
    }

    @Override
    public boolean recordInDB(){ //запись в бд
        boolean result = false;
        String table_name = "mem_info", checker = "select * from " + table_name;
        int diff = 0, last_index = 0, counter = 0, first_index = 0;
        try {
            Connection c = DriverManager.getConnection(this.getUrl(), this.getUser(), this.getPassword());

            String create_table = "create table " + table_name +
                    " ( id serial primary key," +
                    "used_memory real not null," +
                    "total_memory real not null," +
                    "measure varchar(2) not null," +
                    "date varchar(20) not null )";

            String check_table = "select count(*) from information_schema.tables where table_name='" + table_name + "'";

            try {
                Class.forName("org.postgresql.Driver");
                Statement stmt = c.createStatement();
                ResultSet rs = stmt.executeQuery(check_table);
                rs.next();
                if(rs.getInt(1) == 0){
                    stmt.executeUpdate(create_table);
                }
                //System.out.println("table already exist");
                String insert = "insert into " + table_name + " (used_memory, total_memory, measure, date) values ('" + this.getMb_used() + "','" + this.getMb_total() + "','" + this.getMeasure() + "','" + this.getDate() +"')";
                stmt.executeUpdate(insert);
                rs = stmt.executeQuery(checker);
                while(rs.next()) {
                    last_index = rs.getInt("id");
                    counter++;
                    if(counter == 1){
                        first_index = last_index;
                    }
                }
                if(first_index > last_index){
                    stmt.executeUpdate("delete from " + table_name + " where id > " + 0 );
                }
                //System.out.println("last_id = " + last_index);
                if(last_index > this.getMaxCount()){
                    diff = last_index - this.getMaxCount();
                    //System.out.println("del id < " + (diff+1));
                    stmt.executeUpdate("delete from " + table_name + " where id < " + (diff + 1));
                }
                stmt.close();
                //System.out.println("Opened database successfully");
                result = true;
            }
            finally {
                c.close();
            }
        }
        catch (Exception e){
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
            result = false;
        }
        return result;
    }

}
