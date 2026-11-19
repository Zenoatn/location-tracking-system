package br.edu.maua.server.services;

import br.edu.maua.datamanager.dao.ScheduleDao;
import br.edu.maua.datamanager.dao.VehicleDao;
import br.edu.maua.model.ActiveVehicle;
import br.edu.maua.model.ScheduleEntry;
import br.edu.maua.model.VehicleOption;
import br.edu.maua.model.WaitingDriver;
import java.sql.SQLException;
import java.util.List;

public class TransportService {

    private final VehicleDao vehicleDao;
    private final ScheduleDao scheduleDao;

    public TransportService() {
        this(new VehicleDao(), new ScheduleDao());
    }

    TransportService(VehicleDao vehicleDao, ScheduleDao scheduleDao) {
        this.vehicleDao = vehicleDao;
        this.scheduleDao = scheduleDao;
    }

    public List<ActiveVehicle> getActiveVehicles() throws SQLException {
        return vehicleDao.findActiveVehicles();
    }

    public List<ScheduleEntry> getSchedules(String direction, String dayType) throws SQLException {
        return scheduleDao.findActive(direction, dayType);
    }

    public List<VehicleOption> getAvailableVehicles() throws SQLException {
        return vehicleDao.findAvailable();
    }

    public List<WaitingDriver> getWaitingDrivers(String location) throws SQLException {
        return vehicleDao.findWaitingDrivers(location);
    }
}
