package bq.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;

public class Dates {

  public static Optional<ZonedDateTime> asZonedDateTime(int year, int month, int day, ZoneId zone) {
    try {
      return Optional.of(ZonedDateTime.of(year, month, day, 0, 0, 0, 0, zone));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  public static Optional<ZonedDateTime> asZonedDateTime(String s, DateTimeFormatter f) {
    if (S.isBlank(s)) {
      return Optional.empty();
    }
    if (f == null) {
      try {
        ZonedDateTime dt = ZonedDateTime.parse(s);
        return Optional.of(dt);
      } catch (Exception e) {
        return Optional.empty();
      }
    }
    try {
      ZonedDateTime dt = ZonedDateTime.parse(s, f);
      return Optional.ofNullable(dt);
    } catch (Exception ignore) {

    }
    return Optional.empty();
  }

  public static Optional<LocalDate> asLocalDate(String s) {
    return asLocalDate(s, false);
  }

  static Optional<LocalDate> asLocalDate(String s, boolean reentrant) {
    if (S.isBlank(s)) {
      return Optional.empty();
    }
    try {
      LocalDate dt = LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
      return Optional.of(dt);
    } catch (Exception ignore) {

    }
    try {
      LocalDate dt = LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyyMMdd"));
      return Optional.of(dt);
    } catch (Exception ignore) {

    }
    try {
      LocalDate dt = LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyy/MM/dd"));
      return Optional.of(dt);
    } catch (Exception ignore) {

    }
    if (reentrant == false) {
      Optional<LocalDateTime> dt = asLocalDateTime(s, true);
      if (dt.isPresent()) {
        return Optional.of(LocalDate.of(dt.get().getYear(), dt.get().getMonthValue(), dt.get().getDayOfMonth()));
      }
    }

    return Optional.empty();
  }

  public static Optional<LocalDateTime> asLocalDateTime(String s) {
    return asLocalDateTime(s, false);
  }

  public static Optional<LocalDateTime> asLocalDateTime(String s, boolean reentrant) {
    if (S.isBlank(s)) {
      return Optional.empty();
    }
  
    // hack!!!
    if (s.endsWith(".000000Z")) {
      s =s.replace(".000000Z", "");
    }
    try {
      LocalDateTime dt = LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
      return Optional.of(dt);
    } catch (Exception ignore) {

    }
   
    
    
    try {
      if (reentrant == false) {
        Optional<LocalDate> dt = Dates.asLocalDate(s, true);
        if (dt.isEmpty()) {
          return Optional.empty();
        } else {
          return Optional.of(dt.get().atStartOfDay());
        }
      }

    } catch (Exception ignore) {

    }
    return Optional.empty();
  }

  private static Optional<ZonedDateTime> offset(ZonedDateTime dt, ZoneId zone) {
    if (dt == null) {
      return Optional.empty();
    }
    if (zone == null) {
      return Optional.of(dt);
    }
    return Optional.of(dt.withZoneSameInstant(zone));
  }

  public static Optional<ZonedDateTime> asZonedDateTime(String s) {
    return asZonedDateTime(s, (ZoneId) null);
  }

  public static Optional<ZonedDateTime> asZonedDateTime(String s, ZoneId zone) {

    if (S.isBlank(s)) {
      return Optional.empty();
    }
    Optional<ZonedDateTime> dt = asZonedDateTime(s, DateTimeFormatter.ISO_DATE);
    if (dt.isPresent()) {
      return offset(dt.orElse(null), zone);
    }

    dt = asZonedDateTime(s, DateTimeFormatter.ISO_DATE_TIME);
    if (dt.isPresent()) {
      return offset(dt.orElse(null), zone);
    }

    dt = parseNumeric(s);
    if (dt.isPresent()) {
      return offset(dt.orElse(null), zone);
    }

    // everythng after this point was missing zone info, so it requires an implicit
    // zone
    if (zone == null) {
      zone = Zones.UTC;
    }

    Optional<LocalDateTime> ldt = asLocalDateTime(s);
    if (ldt.isPresent()) {
      return Optional.of(ldt.get().atZone(zone));
    }

    Optional<LocalDate> ldo = asLocalDate(s);
    if (ldo.isPresent()) {
      LocalDate ld = ldo.get();
      ZonedDateTime zdt = ZonedDateTime.of(ld.getYear(), ld.getMonthValue(), ld.getDayOfMonth(), 0, 0, 0, 0, zone);
      return Optional.of(zdt);
    }

    return Optional.empty();
  }

  public static LocalDate asLocalDate(ZonedDateTime dt) {
    return LocalDate.ofInstant(dt.toInstant(), dt.getZone());
  }

  public static List<LocalDate> localDateList(ZonedDateTime from, ZonedDateTime to) {
    return localDateList(asLocalDate(from), asLocalDate(to));
  }

  public static List<LocalDate> localDateList(LocalDate from, LocalDate to) {
    List<LocalDate> list = Lists.newArrayList();
    LocalDate d = from;
    while (!d.isAfter(to)) {
      list.add(d);
      d = d.plus(1, ChronoUnit.DAYS);
    }
    return list;
  }

  public static Optional<ZonedDateTime> asZonedDateTime(int year, int month, int day) {
    return Optional.of(ZonedDateTime.of(year, month, day, 0, 0, 0, 0, Zones.UTC));
  }

  public static Optional<ZonedDateTime> asZonedDateTimeUTCStartOfDay(String s) {
    Optional<ZonedDateTime> dt = asZonedDateTime(s, Zones.UTC);
    if (dt.isEmpty()) {
      return dt;
    }
    return Optional.of(dt.get().truncatedTo(ChronoUnit.DAYS));
  }

  static Optional<ZonedDateTime> parseNumeric(String s) {
    if (s == null) {
      return Optional.empty();
    }

    long t = 0;

    try {
      t = Long.parseLong(s.trim());
    } catch (Exception ignore) {
      return Optional.empty();
    }

    try {
      String tString = Long.toString(t);
      if (tString.length() == 8 && tString.startsWith("1") || tString.startsWith("2")) {

        LocalDate dt = LocalDate.parse(tString, DateTimeFormatter.ofPattern("yyyyMMdd"));
        return Optional.of(dt.atStartOfDay(Zones.UTC));
      }

    } catch (Exception IGNORE) {
    }

    try {
      ZonedDateTime cutoff = ZonedDateTime.of(2200, 1, 1, 0, 0, 0, 0, Zones.UTC);

      if (t < cutoff.toEpochSecond()) {
        return Optional.of(ZonedDateTime.ofInstant(Instant.ofEpochSecond(t), Zones.UTC));
      } else {
        return Optional.of(ZonedDateTime.ofInstant(Instant.ofEpochMilli(t), Zones.UTC));
      }
    } catch (Exception e) {

    }

    return Optional.empty();
  }
}
