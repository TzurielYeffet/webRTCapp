const http = require("http")
const Socket = require("websocket").server
const server = http.createServer(()=>{})

const PORT = 3000

server.listen(PORT,()=>{
    console.log(`server is running on port ${PORT} `)
})


const webSocket = new Socket({httpServer:server})
const users=[]


webSocket.on('request',(req)=>{
    const connection = req.accept()
    // console.log(connection) 
    connection.on('message',(message)=>{
        const data = JSON.parse(message.utf8Data)
        console.log(data)
        const user = findUser(data.name)


        switch(data.type){
            case "store_user":
                if(user!=null){
                    connection.send(JSON.stringify({
                        type:"user already exist"
                    }))
                    return
                }
                const newUser = {
                    name:data.name,connection:connection
                }
                users.push(newUser)
                // console.log(users[0].name)
            break

            case "start_call":
                let userToCall = findUser(data.target)
                if(userToCall){
                    connection.send(JSON.stringify({
                        type:"call_response",data:"user is ready for call"
                    }))
                    console.log(data)
                }else{
                    connection.send(JSON.stringify({
                        type:"call_response",data:"user is unavailable"
                    }))
                    console.log(data)

                }
            break;
            case "create_offer":
                let userToReciveOffer = findUser(data.target)
                // console.log(data.data) 
                if(userToReciveOffer){
                    userToReciveOffer.connection.send(JSON.stringify({
                        type:"offer_received",
                        name:data.name,
                        data:data.data.sdp
                    }))
                }
            break

            case "create_answer":
                let userToReciveAnswer = findUser(data.target)
                if(userToReciveAnswer){
                    userToReciveAnswer.connection.send(JSON.stringify({
                        type:"answer_received",
                        name:data.name,
                        data:data.data.sdp
                    }))
                }
            break

            case "ice_candidate":
                let userToReciveIceCandidate= findUser(data.target)
                if(userToReciveIceCandidate){
                    userToReciveIceCandidate.connection.send(JSON.stringify({
                        type:"ice_candidate",
                        name:data.name,
                        data:{
                            sdpMLineIndex:data.data.sdpMLineIndex,
                            sdpMid:data.data.sdpMid,
                            sdpCandidate:data.data.sdpCandidate
                        }
                    }))
                }

                


        }
   })

    connection.on("close",()=>{
        users.forEach(user =>{
            if(user.connection === connection){
                users.splice(users.indexOf(user),1)
            }
        })
    })


 

})


const findUser=username=>{
    for(let i=0;i<users.length;i++){
        if(users[i].name==username){
            return users[i]
        }
    }
}

















