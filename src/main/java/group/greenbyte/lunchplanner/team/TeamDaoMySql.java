package group.greenbyte.lunchplanner.team;

import group.greenbyte.lunchplanner.exceptions.DatabaseException;
import group.greenbyte.lunchplanner.team.database.Team;
import group.greenbyte.lunchplanner.team.database.TeamDatabase;
import group.greenbyte.lunchplanner.team.database.TeamMemberDataForReturn;
import group.greenbyte.lunchplanner.user.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class TeamDaoMySql implements TeamDao {

    private final UserDao userDao;
    private final JdbcTemplate jdbcTemplate;

    private static final String TEAM_TABLE = "team";
    private static final String TEAM_ID = "team_id";
    private static final String TEAM_NAME = "team_name";
    private static final String TEAM_DESCRIPTION = "description";
    private static final String TEAM_PUBLIC = "is_public";
    private static final String TEAM_PARENT = "parent_team";

    public static final String TEAM_MEMBER_TABLE = "team_member";
    public static final String TEAM_MEMBER_USER = "user_name";
    public static final String TEAM_MEMBER_TEAM = "team_id";
    public static final String TEAM_MEMBER_ADMIN = "is_admin";

    @Autowired
    public TeamDaoMySql(UserDao userDao, JdbcTemplate jdbcTemplate) {
        this.userDao = userDao;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public int insertTeam(String teamName, String description, String adminName, boolean isPublic) throws DatabaseException {
        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate);
        simpleJdbcInsert.withTableName(TEAM_TABLE).usingGeneratedKeyColumns(TEAM_ID);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(TEAM_NAME, teamName);
        parameters.put(TEAM_DESCRIPTION, description);
        parameters.put(TEAM_PUBLIC, isPublic);

        try {
            Number key = simpleJdbcInsert.executeAndReturnKey(new MapSqlParameterSource(parameters));

            addAdminToTeam(key.intValue(), adminName);

            return key.intValue();
        } catch (Exception e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public int insertTeamWithParent(String teamName, String description, String adminName, boolean isPublic, int parent) throws DatabaseException {
        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate);
        simpleJdbcInsert.withTableName(TEAM_TABLE).usingGeneratedKeyColumns(TEAM_ID);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(TEAM_NAME, teamName);
        parameters.put(TEAM_DESCRIPTION, description);
        parameters.put(TEAM_PARENT, parent);
        parameters.put(TEAM_PUBLIC, isPublic);

        try {
            Number key = simpleJdbcInsert.executeAndReturnKey(new MapSqlParameterSource(parameters));

            addAdminToTeam(key.intValue(), adminName);

            return key.intValue();
        } catch (Exception e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public Team getTeam(int teamId) throws DatabaseException {
        try {
            String SQL = "SELECT * FROM " + TEAM_TABLE + " WHERE " + TEAM_ID + " = ?";

            List<TeamDatabase> teams = jdbcTemplate.query(SQL,
                    new BeanPropertyRowMapper<>(TeamDatabase.class),
                    teamId);

            if (teams.size() == 0)
                return null;
            else {
                Team team = teams.get(0).getTeam();
                if(teams.get(0).getParentTeam() != null)
                    team.setParentTeam(teams.get(0).getParentTeam());

                team.setInvitations(new HashSet<>(getInvitations(team.getTeamId())));

                return team;
            }
        } catch (Exception e) {
            throw new DatabaseException(e);
        }
    }

    /*@Override
    public Team getTeamWithParent(int teamId) throws DatabaseException {
        try {
            String SQL = "SELECT * FROM " + TEAM_TABLE + " WHERE " + TEAM_ID + " = ?";

            List<TeamDatabase> teams = jdbcTemplate.query(SQL,
                    new BeanPropertyRowMapper<>(TeamDatabase.class),
                    teamId);

            if (teams.size() == 0)
                return null;
            else {
                Team team = teams.get(0).getTeam();
                if(teams.get(0).getParentTeam() != null)
                    team.setParentTeam(getTeamWithParent(teams.get(0).getParentTeam()));

                return team;
            }
        } catch (Exception e) {
            throw new DatabaseException(e);
        }
    }*/

    @Override
    public List<Team> findPublicTeams(String searchword) throws DatabaseException {
        try {
            String SQL = "SELECT * FROM " + TEAM_TABLE + " WHERE ((" +
                    TEAM_NAME + " LIKE ?" +
                    " OR " + TEAM_DESCRIPTION + " LIKE ?)" +
                    " AND " + TEAM_PUBLIC + " = ?)";

            List<TeamDatabase> teams = jdbcTemplate.query(SQL,
                    new BeanPropertyRowMapper<>(TeamDatabase.class),
                    "%" + searchword + "%",
                    "%" + searchword + "%",
                    1);

            List<Team> teamsReturn = new ArrayList<>(teams.size());
            for(TeamDatabase teamDatabase: teams) {
                Team team = teamDatabase.getTeam();

                team.setInvitations(new HashSet<>(getInvitations(team.getTeamId())));

                teamsReturn.add(team);
            }

            return teamsReturn;
        } catch (Exception e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public List<Team> findTeamsUserInvited(String userName, String searchword) throws DatabaseException {
        try {
            String SQL = "select * from " + TEAM_TABLE + " inner join " + TEAM_MEMBER_TABLE + " " + TEAM_MEMBER_TABLE +
                    " on " + TEAM_TABLE + "." + TEAM_ID + " = " + TEAM_MEMBER_TABLE + "." + TEAM_MEMBER_TEAM +
                    " WHERE (" + TEAM_NAME + " LIKE ?" +
                    " OR " + TEAM_DESCRIPTION + " LIKE ?" +
                    ") AND " + TEAM_MEMBER_USER + " = ?";


            List<TeamDatabase> teams = jdbcTemplate.query(SQL,
                    new BeanPropertyRowMapper<>(TeamDatabase.class),
                    "%" + searchword + "%", "%" + searchword + "%", userName);

            List<Team> teamsReturn = new ArrayList<>(teams.size());
            for(TeamDatabase teamDatabase: teams) {
                Team team = teamDatabase.getTeam();

                team.setInvitations(new HashSet<>(getInvitations(team.getTeamId())));

                teamsReturn.add(team);
            }

            return teamsReturn;
        } catch (Exception e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public List<TeamMemberDataForReturn> getInvitations(int teamId) throws DatabaseException {
        try {
            String SQL = "SELECT * FROM " + TEAM_MEMBER_TABLE + " WHERE " +
                    TEAM_MEMBER_TEAM + " = ?";

            return jdbcTemplate.query(SQL,
                    new BeanPropertyRowMapper<>(TeamMemberDataForReturn.class),
                    teamId);
        } catch (Exception e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public void updateTeamIsPublic(int teamId, boolean isPublic) throws DatabaseException {
        String SQL = "UPDATE " + TEAM_TABLE + " SET " + TEAM_PUBLIC + " = ? WHERE " + TEAM_ID + " = ?";

        try {
            jdbcTemplate.update(SQL, isPublic, teamId);
        } catch (Exception e) {
            throw new DatabaseException(e);
        }
    }

    /**
     * Remove a team member from database
     *
     * @param userToRemove user that is going to be deleted from database
     * @param teamId id of the team
     * @throws DatabaseException
     */
    @Override
    public void removeTeamMember(String userToRemove, int teamId) throws DatabaseException {
        String SQL = " DELETE FROM " + TEAM_MEMBER_TABLE + " WHERE " + TEAM_MEMBER_TEAM + " = ? AND "
                + TEAM_MEMBER_USER + " = ? ";

        try {
            jdbcTemplate.update(SQL, teamId, userToRemove);
        } catch (Exception e) {
            throw new DatabaseException(e);
        }
    }

    /**
     * Delete a team
     *
     * @param teamId id of the team
     * @throws DatabaseException
     */
    @Override
    public void deleteTeam(int teamId) throws DatabaseException {
        String SQL = "DELETE FROM " + TEAM_TABLE + " WHERE " + TEAM_ID + " = ? ";

        try {
            jdbcTemplate.update(SQL, teamId);
        } catch (Exception e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public void leave(String username, int teamId) throws DatabaseException {
        String SQL = "DELETE FROM " + TEAM_MEMBER_TABLE + " WHERE " + TEAM_MEMBER_USER + " = ? AND "
                + TEAM_MEMBER_TEAM + " = ?";

        try {
            jdbcTemplate.update(SQL, username, teamId);
           } catch (Exception e) {
            throw new DatabaseException(e);
        }
    }
   
  @Override
    public void updateName(int teamId, String name) throws DatabaseException {
        String SQL = "UPDATE " + TEAM_TABLE + " SET " + TEAM_NAME + " = ? WHERE " + TEAM_ID + " = ?";

        try {
            jdbcTemplate.update(SQL, name, teamId);
        } catch (Exception e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public void updateDescription(int teamId, String description) throws DatabaseException {
        String SQL = "UPDATE " + TEAM_TABLE + " SET " + TEAM_DESCRIPTION + " = ? WHERE " + TEAM_ID + " = ?";

        try {
            jdbcTemplate.update(SQL, description, teamId);
        } catch (Exception e) {
            throw new DatabaseException(e);
        }
    }


    private Team putUserInvited(String userName, int teamId, boolean admin) throws DatabaseException {
        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate);
        simpleJdbcInsert.withTableName(TEAM_MEMBER_TABLE);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(TEAM_MEMBER_ADMIN, admin);
        parameters.put(TEAM_MEMBER_TEAM, teamId);
        parameters.put(TEAM_MEMBER_USER, userName);

        try {
            Number key = simpleJdbcInsert.execute(new MapSqlParameterSource(parameters));
            return getTeam(key.intValue());
        } catch (Exception e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public void addAdminToTeam(int teamId, String userName) throws DatabaseException {
        addUserToTeam(teamId, userName, true);
    }

    @Override
    public void changeUserToAdmin(int teamId, String userName) throws DatabaseException {
        String SQL = "UPDATE " + TEAM_MEMBER_TABLE + " SET " + TEAM_MEMBER_ADMIN + " = ? WHERE " + TEAM_MEMBER_USER +
                " = ? AND " + TEAM_MEMBER_TEAM + " = ?";

        try {
            jdbcTemplate.update(SQL,
                    true, userName, teamId);
        } catch (Exception e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public void addUserToTeam(int teamId, String userName) throws DatabaseException {
        addUserToTeam(teamId, userName, false);
    }

    @Override
    public boolean hasAdminPrivileges(int teamId, String userName) throws DatabaseException {
        try {
            String SQL = "SELECT count(*) FROM "  + TEAM_MEMBER_TABLE + " WHERE " +
                    TEAM_MEMBER_TEAM + " = ? AND " +
                    TEAM_MEMBER_USER + " = ? AND " +
                    TEAM_MEMBER_ADMIN + " = ?";

            int count = jdbcTemplate.queryForObject(SQL,
                    Integer.class,
                    teamId, userName, true);

            return count != 0;
        } catch (Exception e)  {
            throw new DatabaseException(e);
        }
    }

    public boolean isTeamPublic(int teamId) throws DatabaseException {
        Team team = getTeam(teamId);
        if(team != null) {
            return team.isPublic();
        }

        return false;
    }

    @Override
    public boolean hasViewPrivileges(int teamId, String userName) throws DatabaseException {
        if(isTeamPublic(teamId))
            return true;

        try {
            String SQL = "SELECT count(*) FROM "  + TEAM_MEMBER_TABLE + " WHERE " +
                    TEAM_MEMBER_TEAM + " = ? AND " +
                    TEAM_MEMBER_USER + " = ?";

            int count = jdbcTemplate.queryForObject(SQL,
                    Integer.class,
                    teamId, userName);

            return count != 0;
        } catch (Exception e)  {
            throw new DatabaseException(e);
        }
    }

    private void addUserToTeam(int teamId, String userName, boolean admin) throws DatabaseException {
        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate);
        simpleJdbcInsert.withTableName(TEAM_MEMBER_TABLE);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(TEAM_MEMBER_USER, userName);
        parameters.put(TEAM_MEMBER_ADMIN, admin);
        parameters.put(TEAM_MEMBER_TEAM, teamId);

        try {
            simpleJdbcInsert.execute(new MapSqlParameterSource(parameters));
        } catch (Exception e) {
            throw new DatabaseException(e);
        }
    }
}
