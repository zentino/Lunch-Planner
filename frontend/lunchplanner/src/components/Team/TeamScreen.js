import PropTypes from "prop-types";
import Loadable from "react-loading-overlay";
import {withStyles} from "@material-ui/core/styles/index";
import React from 'react';
import Dialog from "../Dialog";
import {Button, Slide, Divider, CircularProgress} from "@material-ui/core";
import {getUsername, setAuthenticationHeader} from "../authentication/LoginFunctions";
import {getHistory} from "../../utils/HistoryUtils"
import {Https as SecretIcon} from "@material-ui/icons";
import {Public as PublicIcon} from "@material-ui/icons";
import UserList from "../User/UserList";
import {
    getTeam,
    changeTeamDescription,
    changeTeamName,
    removeUserFromTeam,
    inviteMemberToTeam,
    joinTeam,
    leaveTeam
} from "./TeamFunctions";
import {teamListNeedReload} from "./TeamList";
import TextFieldEditing from "../editing/TextFieldEditing";
import axios from "axios";
import {HOST} from "../../Config";
import {Link} from "react-router-dom";
import {Add} from "@material-ui/icons";


function Transition(props) {
    return <Slide direction="up" {...props} />;
}

const styles = {
    appBar: {
        position: 'relative',
    },
    flex: {
        flex: 1,
    },
    textField: {
        marginTop:20,
        marginBottom:30,
        marginLeft: 20,
        width: "90%",
    },
    error: {
        textAlign: 'center',
        color: '#ff7700',
        marginTop: '10px',
        marginBottom: '0px',
    },
    header: {
        backgroundColor: '#1EA185',
        height: '100px',
        color: 'white',
        padding: '16px',
        fontFamily: 'Work Sans',
        fontSize: '16px',
        lineHeight: '24px',
    },
    button:{
        fontSize: '16px',
        fontFamily: 'Work Sans',
        color: "white",
        bottom: 0,
        width: "100%",
        minHeight: '56px',
        zIndex: '10000',
    },
    buttonInvitation: {
        position: "fixed",
        zIndex: '10000',
    },
    fontBig: {
        fontSize: '20px',
        margin: '0px',
        width: '100%',
    },
    fontSmall: {
        fontSize: '11px',
        margin: '0px',
    },
    icons: {
        height: '13px',
        width: '13px',
    },
    headerText: {
        float: 'left',
    },
    picture:{
        float: 'left',
        border: '1px black solid',
        borderRadius: '50%',
        height: '64px',
        width: '64px',
    },
    content: {
        width: '100%',
    },
    information:{
        height: '160px',
        width: 'atuo',
        marginLeft: '24px',
        marginRight: '24px',
        marginTop: '24px',
    },
    teamName: {
        width: '100%',
    },
    description: {
        paddingTop: '10px',
        marginTop: '15px',
        fontSize: '16px',
        width: '100%',
    },
    secretTeam:{
        marginTop: '20px',
        marginLeft: '20px',
        color: '#1EA185',
    },
    secretTeamText:{
        marginLeft: '40px',
        marginTop: '-30px',
        width: '50%',
    },
    divider:{
        width: '100%',
    },
    invitations: {
        marginLeft: '0px',
        marginTop: '8px',
    },
    invitaionsHeader: {
        marginLeft: '16px',
        marginBottom: '0px',
        fontSize: '16px',
        fontWeight: '500',
        lineHeight: '24px',
    },
    overButton: {
        height: '100%',
        overflowY: 'auto',
        display: 'flex',
        flexDirection: 'column',
    },
    // ADD NEW PEOPLE
    addNewPeopleRoot: {
        height: '72px',
        padding: '20px 16px',
        backgroundColor: '#f3f3f3',
        "&:hover": {
            cursor: 'pointer',
        },
    },
    newPeopleIcon: {
        height: '32px',
        float: 'left',
    },
    newPeopleText: {
        marginTop: '6px',
        marginLeft: '57px',
    },
};

class TeamScreen extends React.Component {

    constructor(props) {
        super();
        setAuthenticationHeader();
        this.state = {
            teamId: 0,
            open: true,
            name:"",
            description: "",
            people:[],
            loading: true,
            isPublic: false,
        };

    }

    componentDidMount() {
        let teamId, teamName, description, people, isPublic;

        teamId = this.props.match.params.teamId;

        if(this.props.location.query) {
            console.log("Query exists");
            if (this.props.location.query.teamName) {
                teamName = String(this.props.location.query.teamName);
            }
            if (this.props.location.query.description) {
                description = String(this.props.location.query.description);
            }
            if (this.props.location.query.people) {
                people = this.props.location.query.people;
            }
            if (this.props.location.query.public) {
                isPublic = this.props.location.query.public;
            }

            this.setState({
                teamId: teamId,
                name: teamName,
                description: description,
                people: people,
                loading: false,
                public: isPublic,
            })

        } else {
            console.log("Query does not exists");
            this.loadTeam(teamId);
        }
    }

    parseUrl = () => {
        const params = new URLSearchParams(this.props.location.search);
        let invitedUsers = params.get('invitedUsers');
        let invitedTeams = params.get('invitedTeams');
        let teamMember = params.get('teamMember');

        let usersToInvite = [];

        if(invitedUsers) {
            usersToInvite = usersToInvite.concat(invitedUsers.split(','));
        }
        if(teamMember) {
            usersToInvite = usersToInvite.concat(teamMember.split(','));
        }

        if(usersToInvite.length !== 0) {
            //remove doubles and already invited people
            let usersToInviteUnique = usersToInvite.filter((item, pos) => {
                return usersToInvite.indexOf(item) === pos && !this.state.people.some((person) => person.userName === item);
            });

            inviteMemberToTeam(this.state.teamId, usersToInviteUnique, (user) => {
                let allUsers = this.state.people;
                allUsers.push({userName: user, admin: false});
                this.setState({
                    people: allUsers,
                });
            })
        }
    };

    loadTeam = (teamId) => {
        this.setState({
            loading: true,
        });
        if(!teamId)
            teamId = this.state.teamId;

        getTeam(teamId, (response) => {
            this.setState({
                teamId: response.data.teamId,
                name: response.data.teamName,
                description: response.data.description,
                people: response.data.invitations,
                public: response.data.public,
                loading: false,
                isPublic: response.data.public,
            });
        })
    };

    handleLeave = () => {
        getHistory().push("/app/team");
        let people = this.state.people;
        let index = people.indexOf(getUsername());
        people.splice(index, 1);
        this.setState({
           people: people,
        });
        this.sendAnswer("leave");
    };

    handleJoin = () => {
        console.log("handleJoin")
        getHistory().push("/app/team");
        let people = this.state.people;
        people.push(getUsername());
        this.setState({
            people: people,
        });
        this.sendAnswer("join");
    };

    sendAnswer = (answer) => {
        console.log(answer)
        if(answer === "leave"){
            console.log(answer)
            leaveTeam(this.state.teamId, () => {
                teamListNeedReload();
            })
        }else if(answer === "join"){
            console.log(answer)
            joinTeam(this.state.teamId, () => {
                this.loadTeam(this.state.teamId);
                teamListNeedReload();
            })
        }
    };

    onTitleChanged = (event) => {
        this.setState({
            name: event.target.value,
        });

        changeTeamName(this.state.teamId, event.target.value, this.reloadTeamsOnSuccess);
    };

    onDescriptionChanged = (event) => {
        this.setState({
            description: event.target.value,
        });

        changeTeamDescription(this.state.teamId, event.target.value, this.reloadTeamsOnSuccess);
    };

    reloadTeamsOnSuccess = (response) => {
        console.log("reload", response.status)
        if(response.status === 204 || response.status === 201) {
            teamListNeedReload();
        }
    };

    clickRemove = (username) => {
        let people = this.state.people;
        people = people.filter(listValue => listValue.userName !== username)
        this.setState({
            people: people,
        });

        removeUserFromTeam(this.state.teamId, username, this.reloadTeamsOnSuccess)
    };

    render() {
        const { classes } = this.props;
        const error = this.state.error;
        let name = this.state.name;
        let description = this.state.description;
        let people = this.state.people;
        let iAmAdmin = false;
        let userName = getUsername();
        let loading = this.state.loading;
        let isPublic = this.state.isPublic;
        console.log("isPublic", isPublic);

        if(people.length !== 0) {
            this.parseUrl();
        }

        people.sort((a, b) => {
            if(a.answer === 0 && b.answer !== 0) {
                return -1;
            } else if(a.answer !== 0 && b.answer === 0) {
                return 1;
            } else {
                return 0;
            }
        });

        people.forEach((listValue) => {
            if(listValue.userName === userName) {
                if(listValue.admin) {
                    iAmAdmin = true;
                }
            }
        });

        let selectedUsers = [];
        let buttonText = "Join Team";
        let username = getUsername();
        let isInTeam = false;

        people.forEach((listValue) => {
            if(listValue.userName === username) {
                buttonText = "Leave Team";
                isInTeam = true;
            }
        });

        let clickRemove;
        if(iAmAdmin)
            clickRemove = this.clickRemove;

        return (
            <div >
                {loading ?
                        <CircularProgress className={classes.progress} color="secondary"/>
                    :
                <Dialog
                    title={name}
                    closeUrl="/app/team"
                >
                    <div className={classes.overButton}>
                        <div className={classes.content}>
                            <div className={classes.information}>
                                <div className={classes.teamName}>
                                    <p className={classes.fontSmall}>Team Name</p>
                                    <TextFieldEditing onChange={this.onTitleChanged} value={name} editable={iAmAdmin} className={classes.fontBig} />
                                </div>
                                <div className={classes.description}>
                                    <p className={classes.fontSmall}>Description</p>
                                    <TextFieldEditing rowsMax="3" onChange={this.onDescriptionChanged} value={description} editable={iAmAdmin} className={classes.description}  multiline/>
                                </div>
                            </div>

                            <div className={classes.secretTeam}>
                                {isPublic ?
                                    <div>
                                        <PublicIcon/>
                                        <p className={classes.secretTeamText}>Public team. All people can see the activity of this team.</p>
                                    </div>
                                :
                                    <div>
                                        <SecretIcon/>
                                        <p className={classes.secretTeamText}>Secret team. Only you can see the activity of this team.</p>
                                    </div>
                                }

                            </div>
                            <Divider className={classes.divider} />

                            <div className={classes.invitations}>
                                <p className={classes.invitaionsHeader}> Team Member ({people.length})</p>
                                {
                                    (iAmAdmin)
                                        ? <Link to={{pathname: `/app/team/${this.state.teamId}/invite`,  query: {
                                                source: "/app/team/" + this.state.teamId,
                                                invitedUsers: people.map((value) => value.userName).join(','),
                                            }}}>
                                            <div className={classes.addNewPeopleRoot}>
                                                <Add className={classes.newPeopleIcon} />
                                                <p className={classes.newPeopleText}>Add more people...</p>
                                            </div>
                                        </Link>
                                        : ''
                                }
                                <UserList
                                    selectedUsers={people.map(value => value.userName)}
                                    othersInvited={false}
                                    selectable={false}
                                    users={people}
                                    clickRemove={clickRemove}
                                />
                            </div>
                        </div>
                    </div>
                    {isInTeam?
                        <Button variant="raised"
                                        color="secondary"
                                        onClick={this.handleLeave}
                                        className={classes.button}>
                        {buttonText}
                    </Button>
                    :
                        <Button variant="raised"
                                color="secondary"
                                onClick={this.handleJoin}
                                className={classes.button}>
                            {buttonText}
                        </Button>
                    }

                </Dialog>}
            </div>
        );
    }
}
TeamScreen.propTypes = {
    classes: PropTypes.object.isRequired,
    id: PropTypes.number.isRequired,
    isAdmin: PropTypes.bool.isRequired,
    name:PropTypes.string.isRequired,
    description: PropTypes.string.isRequired,
    people:PropTypes.object.isRequired,

};
export default withStyles(styles, { withTheme: true })(TeamScreen);