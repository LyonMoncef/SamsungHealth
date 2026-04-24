"""encrypt mood Art.9 fields (V2.2)

Revision ID: cd52629cf7b7
Revises: ffc3fa072656
Create Date: 2026-04-24 05:36:27.729447

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa

from server.db.encrypted import EncryptedInt, EncryptedString


# revision identifiers, used by Alembic.
revision: str = 'cd52629cf7b7'
down_revision: Union[str, Sequence[str], None] = 'ffc3fa072656'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """V2.2 — chiffrement Art.9 sur mood. Assume fresh DB (rows TEXT/INT droppées via USING NULL)."""
    op.add_column('mood', sa.Column('mood_type_crypto_v', sa.Integer(), server_default='1', nullable=False))
    op.add_column('mood', sa.Column('emotions_crypto_v', sa.Integer(), server_default='1', nullable=False))
    op.add_column('mood', sa.Column('factors_crypto_v', sa.Integer(), server_default='1', nullable=False))
    op.add_column('mood', sa.Column('notes_crypto_v', sa.Integer(), server_default='1', nullable=False))
    # Conversion type TEXT/INT → BYTEA : USING NULL pour vider les anciennes valeurs (fresh DB assumée).
    # Toute donnée mood pré-V2.2 doit être ré-importée via le pipeline CSV.
    op.alter_column('mood', 'mood_type',
                    existing_type=sa.INTEGER(),
                    type_=EncryptedInt(),
                    existing_nullable=True,
                    postgresql_using='NULL')
    op.alter_column('mood', 'emotions',
                    existing_type=sa.TEXT(),
                    type_=EncryptedString(),
                    existing_nullable=True,
                    postgresql_using='NULL')
    op.alter_column('mood', 'factors',
                    existing_type=sa.TEXT(),
                    type_=EncryptedString(),
                    existing_nullable=True,
                    postgresql_using='NULL')
    op.alter_column('mood', 'notes',
                    existing_type=sa.TEXT(),
                    type_=EncryptedString(),
                    existing_nullable=True,
                    postgresql_using='NULL')


def downgrade() -> None:
    """V2.2 — downgrade ne préserve PAS les données chiffrées (perte irrémédiable)."""
    op.alter_column('mood', 'notes',
                    existing_type=EncryptedString(),
                    type_=sa.TEXT(),
                    existing_nullable=True,
                    postgresql_using='NULL')
    op.alter_column('mood', 'factors',
                    existing_type=EncryptedString(),
                    type_=sa.TEXT(),
                    existing_nullable=True,
                    postgresql_using='NULL')
    op.alter_column('mood', 'emotions',
                    existing_type=EncryptedString(),
                    type_=sa.TEXT(),
                    existing_nullable=True,
                    postgresql_using='NULL')
    op.alter_column('mood', 'mood_type',
                    existing_type=EncryptedInt(),
                    type_=sa.INTEGER(),
                    existing_nullable=True,
                    postgresql_using='NULL')
    op.drop_column('mood', 'notes_crypto_v')
    op.drop_column('mood', 'factors_crypto_v')
    op.drop_column('mood', 'emotions_crypto_v')
    op.drop_column('mood', 'mood_type_crypto_v')
