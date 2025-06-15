from fastapi import FastAPI, Depends
from sqlalchemy.orm import Session
import threading

import models, schemas, consumer
from database import engine, SessionLocal, get_db

# Veritabanı tablolarını oluştur
models.Base.metadata.create_all(bind=engine)

app = FastAPI(
    title="Notification Microservice",
    description="Sipariş olaylarını dinleyerek bildirimleri yöneten servis.",
    version="1.0.0"
)

@app.on_event("startup")
def on_startup():
    """Uygulama başladığında RabbitMQ consumer'ını başlat."""
    print("API sunucusu başlatıldı.")
    consumer_thread = threading.Thread(target=consumer.start_consumer, args=(SessionLocal,))
    consumer_thread.daemon = True  # Ana thread kapanınca bu da kapansın
    consumer_thread.start()

@app.get("/notifications/", response_model=list[schemas.NotificationSchema])
def read_notifications(skip: int = 0, limit: int = 100, db: Session = Depends(get_db)):
    """
    Veritabanında kayıtlı tüm bildirimleri listeler.
    """
    notifications = db.query(models.Notification).offset(skip).limit(limit).all()
    return notifications