/**
 * 운영용 스마트 이미지 시스템 - 다양한 이미지 소스 지원
 */
export function useSmartImages() {
    const BASE_IMAGE_PATH = '/images/banners/products/'
    const API_GATEWAY_URL = 'http://13.209.253.241:8080'

    const getProductImage = (product) => {
        if (!product) {
            return `${BASE_IMAGE_PATH}default-product.jpg`
        }

        // 1. 다양한 이미지 필드명 체크 (우선순위 순)
        const imageFields = [
            'mainImage',
            'image',
            'imageUrl',
            'productImage',
            'thumbnail',
            'picture'
        ]

        let imageUrl = null
        for (const field of imageFields) {
            if (product[field] && typeof product[field] === 'string' && product[field].trim()) {
                imageUrl = product[field].trim()
                break
            }
        }

        // 2. 이미지 URL이 있는 경우 처리
        if (imageUrl) {
            // DB의 /upload/ 경로인 경우 (가장 중요한 수정 부분)
            if (imageUrl.startsWith('/upload/')) {
                // /upload/0ce36f4e-c114-4bae-b198-c6010a9b7816.jpg -> /images/0ce36f4e-c114-4bae-b198-c6010a9b7816.jpg
                // /upload/product/main/filename.jpg -> /images/filename.jpg

                let fileName = imageUrl

                // /upload/product/main/ 패턴인 경우 파일명만 추출
                if (imageUrl.includes('/upload/product/main/')) {
                    fileName = imageUrl.split('/').pop()
                    return `/images/${fileName}`
                }
                // /upload/ 바로 다음에 파일명이 오는 경우
                else if (imageUrl.startsWith('/upload/')) {
                    fileName = imageUrl.replace('/upload/', '')
                    return `/images/${fileName}`
                }
            }

            // DB의 /upload/product/main/ 경로인 경우 (기존 코드 유지 - 혹시 모르니)
            if (imageUrl.startsWith('/upload/product/main/')) {
                const fileName = imageUrl.split('/').pop()
                return `/images/${fileName}`
            }

            // 상대 경로인 경우 (/images/로 시작) - 이미 올바른 경로
            if (imageUrl.startsWith('/images/')) {
                return imageUrl
            }

            // 외부 URL인 경우 (http로 시작)
            if (imageUrl.startsWith('http')) {
                return imageUrl
            }

            // API Gateway를 통한 이미지인 경우
            if (imageUrl.startsWith('/api/images/')) {
                return `${API_GATEWAY_URL}${imageUrl}`
            }

            // 파일명만 있는 경우 (확장자 있음)
            if (!imageUrl.includes('/') && imageUrl.includes('.')) {
                return `/images/${imageUrl}`
            }

            // 기타 상대 경로 - /images/ 경로로 변환
            if (!imageUrl.startsWith('http')) {
                // 맨 앞의 '/' 제거 후 /images/ 추가
                const cleanPath = imageUrl.startsWith('/') ? imageUrl.substring(1) : imageUrl
                return `/images/${cleanPath}`
            }
        }

        // 3. 카테고리별 기본 이미지 (상품명이나 카테고리 정보가 있는 경우)
        const productName = product.name || product.title || product.productName || ''
        const categoryId = product.categoryId || product.category_id

        if (productName) {
            // 상품명 기반 임시 이미지 생성
            const nameHash = productName.length % 10 + 1
            const tempImage = `https://picsum.photos/300/300?random=${nameHash}`
            return tempImage
        }

        // 4. 카테고리별 기본 이미지
        if (categoryId) {
            const categoryImages = {
                1: 'vegetables.jpg',
                2: 'processed-food.jpg',
                3: 'convenience.jpg',
                4: 'bakery.jpg',
                5: 'dairy.jpg'
            }
            const categoryImage = categoryImages[categoryId]
            if (categoryImage) {
                return `/images/${categoryImage}`
            }
        }

        // 5. 최종 기본 이미지
        return `${BASE_IMAGE_PATH}default-product.jpg`
    }

    const handleImageError = (event) => {
        const currentSrc = event.target.src

        // 이미 기본 이미지인 경우 picsum으로 변경
        if (currentSrc.includes('default-product.jpg')) {
            event.target.src = 'https://picsum.photos/300/300?random=1'
        }
        // picsum도 실패한 경우 placeholder
        else if (currentSrc.includes('picsum.photos')) {
            event.target.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzAwIiBoZWlnaHQ9IjMwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZGRkIi8+PHRleHQgeD0iNTAlIiB5PSI1MCUiIGZvbnQtZmFtaWx5PSJBcmlhbCIgZm9udC1zaXplPSIxNCIgZmlsbD0iIzk5OSIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZHk9Ii4zZW0iPuydtOuvuOyngCDsl4bsnYw8L3RleHQ+PC9zdmc+'
        }
        // 기본 이미지로 교체
        else {
            event.target.src = `${BASE_IMAGE_PATH}default-product.jpg`
        }
    }

    const handleImageLoad = (event) => {
        event.target.style.opacity = '1'
    }

    return {
        getProductImage,
        handleImageError,
        handleImageLoad
    }
}