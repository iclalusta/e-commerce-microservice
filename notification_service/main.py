from fastapi import FastAPI, Depends, Header, HTTPException, APIRouter
from sqlalchemy.orm import Session
import threading
from typing import List, Optional

# Diğer modülleri import ediyoruz
import models
import schemas
import consumer
from database import engine, SessionLocal, get_db

# Veritabanı tablolarını oluştur
models.Base.metadata.create_all(bind=engine)

app = FastAPI(
    title="Notification Microservice",
    description="Sipariş olaylarını dinleyerek bildirimleri yöneten servis.",
    version="1.1.0"
)

# --- YENİ: API Gateway kuralına uymak için bir router oluşturuyoruz ---
# Tüm endpoint'ler /api/notification ön ekiyle başlayacak.
router = APIRouter(prefix="/api/notification")


@app.on_event("startup")
def on_startup():
    """Uygulama başladığında RabbitMQ consumer'ını başlat."""
    print("API sunucusu başlatıldı.")
    consumer_thread = threading.Thread(target=consumer.start_consumer, args=(SessionLocal,))
    consumer_thread.daemon = True  # Ana thread kapanınca bu da kapansın
    consumer_thread.start()


# --- GÜNCELLENDİ: Artık X-User-Id header'ını okuyor ---
@router.get("/", response_model=List[schemas.NotificationSchema])
def read_user_notifications(
    x_user_id: Optional[str] = Header(None, alias="X-User-Id"),
    db: Session = Depends(get_db)
):
    """
    Gelen istekteki X-User-Id header'ına göre kullanıcının bildirimlerini listeler.
    """
    # Header eksikse hata döndür
    if x_user_id is None:
        raise HTTPException(
            status_code=400,
            detail="Bu endpoint için X-User-Id header'ı zorunludur."
        )

    # Veritabanından sadece o kullanıcıya ait bildirimleri filtrele
    notifications = db.query(models.Notification).filter(models.Notification.user_id == x_user_id).all()
    
    return notifications


# --- YENİ: Admin/Debug için tüm bildirimleri listeleyen endpoint ---
@router.get("/all", response_model=List[schemas.NotificationSchema])
def read_all_notifications(skip: int = 0, limit: int = 100, db: Session = Depends(get_db)):
    """
    (Admin/Debug) Sistemdeki tüm bildirimleri listeler.
    """
    notifications = db.query(models.Notification).offset(skip).limit(limit).all()
    return notifications


# Router'ı ana uygulamaya dahil et
app.include_router(router)
