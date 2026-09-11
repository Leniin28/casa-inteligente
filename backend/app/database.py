from sqlalchemy import Engine
from sqlmodel import SQLModel, create_engine

from app import models  # noqa: F401  (registra las tablas en SQLModel.metadata)


def create_db_engine(database_url: str) -> Engine:
    connect_args = {"check_same_thread": False} if database_url.startswith("sqlite") else {}
    return create_engine(database_url, connect_args=connect_args)


def init_db(engine: Engine) -> None:
    SQLModel.metadata.create_all(engine)
