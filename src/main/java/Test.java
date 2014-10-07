import lombok.*;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.excel.RowCallbackHandler;
import org.springframework.batch.item.excel.Sheet;
import org.springframework.batch.item.excel.mapping.DefaultRowMapper;
import org.springframework.batch.item.excel.poi.PoiItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.core.io.ClassPathResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;

import javax.persistence.Column;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by fvalmeida on 9/9/14.
 */
public class Test {

    public static void main(String[] args) throws Exception {
        Date startTime = new Date();
        PoiItemReader<Player> itemReader = new PoiItemReader();
        itemReader.setLinesToSkip(1); //First line is column names
//        itemReader.setResource(new ClassPathResource("/Player.xls"));
        itemReader.setResource(new ClassPathResource("/Player.xls"));
        DefaultRowMapper<Player> defaultRowMapper = new DefaultRowMapper();
//        defaultRowMapper.setFieldSetMapper(new PlayerMapper());
        BeanWrapperFieldSetMapper mapper = new BeanWrapperFieldSetMapper();
        mapper.setTargetType(Player.class);
        mapper.setStrict(false);
        mapper.setDistanceLimit(0);
        defaultRowMapper.setFieldSetMapper(mapper);

                itemReader.setRowMapper(new DefaultRowMapper(Player.class));
//                itemReader.setRowMapper(defaultRowMapper);
        itemReader.setSkippedRowsCallback(new RowCallbackHandler() {
            public void handleRow(final Sheet sheet, final String[] row) {
                System.out.println("Skipping: " + StringUtils.arrayToCommaDelimitedString(row));
            }
        });
        itemReader.afterPropertiesSet();
        itemReader.open(new ExecutionContext());

        Player row = null;
        List<Player> list = new ArrayList<Player>();
        List<ErrorMessage> errors = new ArrayList<ErrorMessage>();
        row = new Player(); // avoid problems when there's an error on the first line
        do {
            try{
                row = itemReader.read();
                if (row != null){
                    System.out.println("Read: " + row.toString());
                    list.add(row);
                }
            }catch(RuntimeException e){
                errors.add(new ErrorMessage(e.getCause().getMessage(), itemReader.getCurrentRowIndex()+ 1));
            }
        } while (row != null);
        for (ErrorMessage e : errors){
            System.out.println(e.toString());
        }
        System.out.println("Total of players read: " + list.size());
        Date finalTime = new Date();
        Long time = finalTime.getTime() - startTime.getTime();
        System.out.println("Time executing file: " + time + "ms");
    }

    @Setter
    @Getter
    @ToString
    public static class Player implements Serializable {
        @Column(nullable = false)
        private String id;
        //private String asdf;
        private String lastName;
        private String firstName;
        private String position;
        private int birthYear;
        private int debutYear;
        @DateTimeFormat(pattern = "MM-dd-yyyy")
        private Date birthDate;
        @DateTimeFormat
        private Timestamp anotherDate;

    }

    @Getter
    @Setter
    @ToString
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ErrorMessage {
        private String message;
        private int row;
    }

    public static class PlayerMapper implements FieldSetMapper<Player> {
        public Player mapFieldSet(FieldSet fs) {

            if (fs == null) {
                return null;
            }

            Player player = new Player();
            player.setId(fs.readString("id"));
            player.setLastName(fs.readString("lastName"));
            player.setFirstName(fs.readString("firstName"));
            player.setPosition(fs.readString("position"));
            player.setDebutYear(fs.readInt("debutYear"));
            player.setBirthYear(fs.readInt("birthYear"));

            return player;
        }
    }
}
