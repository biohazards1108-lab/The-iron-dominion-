/*
=================================================
IRON DOMINION SERVER STATUS CONNECTOR
Tekkit 1.6.4 Website

Current:
Reads server data from api/server.json

Future:
Connect directly to Minecraft API
=================================================
*/


async function updateServerStatus() {


    try {


        const response = await fetch(
            "api/server.json"
        );


        const server = await response.json();



        // SERVER ONLINE STATUS

        const status =
        document.getElementById(
            "server-status"
        );


        if(server.online){


            status.innerHTML =
            "🟢 ONLINE";


        }
        else{


            status.innerHTML =
            "🔴 OFFLINE";


        }




        // PLAYER COUNT

        const players =
        document.getElementById(
            "players"
        );


        if(players){


            players.innerHTML =

            server.players
            +
            " / "
            +
            server.maxPlayers;


        }




        // TPS

        const tps =
        document.getElementById(
            "tps"
        );


        if(tps){


            tps.innerHTML =
            server.tps;


        }




    }

    catch(error){


        console.error(

        "Iron Dominion API Error:",
        error

        );


        const status =
        document.getElementById(
            "server-status"
        );


        if(status){


            status.innerHTML =
            "🔴 OFFLINE";


        }


    }



}



// RUN WHEN PAGE LOADS

updateServerStatus();



// AUTO UPDATE EVERY 30 SECONDS

setInterval(

updateServerStatus,

30000

);
