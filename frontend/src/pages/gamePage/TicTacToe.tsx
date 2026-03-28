import React, { useEffect, useState } from 'react';
import { Client } from '@stomp/stompjs';

interface TicTacToeGameProps {
    gameId: string;
    currentUser: string;
}

interface GameData {
    board: (string | null)[];
    currentTurn: string;
    status: string;
}

const TicTacToeGame: React.FC<TicTacToeGameProps> = ({ gameId, currentUser }) => {
    const [board, setBoard] = useState<(string | null)[]>(Array(9).fill(null));
    const [currentTurn, setCurrentTurn] = useState<string>("X");
    const [status, setStatus] = useState<string>("WAITING_FOR_OPPONENT");
    const [stompClient, setStompClient] = useState<Client | null>(null);

    const isMyTurn = currentTurn === currentUser;

    useEffect(() => {
        const client = new Client({
            brokerURL: 'ws://localhost:8080/ws-game',
            
            debug: function (str: string) {
                console.log('STOMP: ' + str);
            },
            
            onConnect: () => {
                console.log('Połączono z serwerem gry!');

                client.subscribe(`/topic/game.${gameId}`, (message) => {
                    const gameData: GameData = JSON.parse(message.body);
                    setBoard(gameData.board);
                    setCurrentTurn(gameData.currentTurn);
                    setStatus(gameData.status);
                    
                    if (gameData.status === 'FINISHED') {
                        alert("Gra zakończona!");
                    }
                });

                client.subscribe('/user/queue/errors', (message) => {
                    alert("Niedozwolony ruch: " + message.body);
                });
            },
            
            onStompError: (frame) => {
                console.error('Błąd STOMP: ', frame.headers['message']);
            }
        });

        client.activate();
        setStompClient(client);

        return () => {
            if (client.active) {
                client.deactivate();
                console.log('Rozłączono z serwerem gry');
            }
        };
    }, [gameId]);

    const handleCellClick = (index: number) => {
        if (!isMyTurn || board[index] !== null || status !== 'IN_PROGRESS') {
            return; 
        }

        if (stompClient && stompClient.connected) {
            stompClient.publish({
                destination: '/app/game.move',
                body: JSON.stringify({
                    gameId,
                    position: index
                })
            });
        }
    };

    return (
        <div className="game-container">
            <h2>Status: {status}</h2>
            <h3>Tura: {isMyTurn ? "Twój ruch" : "Czekaj na przeciwnika..."}</h3>
            
            <div className="board" style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 100px)', gap: '5px' }}>
                {board.map((cell, index) => (
                    <div 
                        key={index} 
                        onClick={() => handleCellClick(index)}
                        style={{ 
                            width: '100px', 
                            height: '100px', 
                            border: '1px solid black', 
                            display: 'flex', 
                            alignItems: 'center', 
                            justifyContent: 'center', 
                            fontSize: '2em', 
                            cursor: isMyTurn && !cell ? 'pointer' : 'not-allowed' 
                        }}
                    >
                        {cell}
                    </div>
                ))}
            </div>
        </div>
    );
};

export default TicTacToeGame;